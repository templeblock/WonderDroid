/* Mednafen - Multi-system Emulator
 *
 * This program is free software; you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation; either version 2 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program; if not, write to the Free Software
 * Foundation, Inc., 59 Temple Place, Suite 330, Boston, MA  02111-1307  USA
 */

//#include <stdio.h>
#include <string.h>

extern "C" {
#include "uk_org_cardboardbox_wonderdroid_WonderSwan.h"
}

#include "log.h"

#include "wswan.h"
#include <stdint.h>
#include "sound.h"
#include "v30mz.h"
#include "memory.h"

#include "../blip/Blip_Buffer.h"

static Blip_Synth<blip_med_quality, 256> WaveSynth;
static Blip_Synth<blip_low_quality, 256> NoiseSynth;
static Blip_Synth<blip_med_quality, 256 * 15> VoiceSynth;

static Blip_Buffer sbuf[2];
//static int16 WaveIL[8000];

static uint16_t period[4];
static uint8_t volume[4]; // left volume in upper 4 bits, right in lower 4 bits
static uint8_t voice_volume;

static uint8_t sweep_step, sweep_value;
static uint8_t noise_control;
static uint8_t control;
static uint8_t output_control;

static int32_t sweep_8192_divider;
static uint8_t sweep_counter;
static uint8_t SampleRAMPos;

static int32_t sample_cache[4][2];

static int32_t last_v_val;

static uint8_t HyperVoice;
static int32_t last_hv_val;

static int32_t period_counter[4];
static int32_t last_val[4][2]; // Last outputted value, l&r
static uint8_t sample_pos[4];
static uint16_t nreg;
static uint32_t last_ts;

#define MK_SAMPLE_CACHE	\
   {    \
    int sample; \
    sample = (((wsRAM[((SampleRAMPos << 6) + (sample_pos[ch] >> 1) + (ch << 4)) ] >> ((sample_pos[ch] & 1) ? 4 : 0)) & 0x0F)) - 0x8;    \
    sample_cache[ch][0] = sample * ((volume[ch] >> 4) & 0x0F);        \
    sample_cache[ch][1] = sample * ((volume[ch] >> 0) & 0x0F);        \
   }

#define MK_SAMPLE_CACHE_NOISE	\
   {    \
    int sample; \
    sample = ((nreg & 1) ? 0xF : 0x0) - 0x8;	\
    sample_cache[ch][0] = sample * ((volume[ch] >> 4) & 0x0F);        \
    sample_cache[ch][1] = sample * ((volume[ch] >> 0) & 0x0F);        \
   }

#define SYNCSAMPLE(wt)	\
   {	\
    int32_t left = sample_cache[ch][0], right = sample_cache[ch][1];	\
    WaveSynth.offset_inline(wt, left - last_val[ch][0], &sbuf[0]);	\
    WaveSynth.offset_inline(wt, right - last_val[ch][1], &sbuf[1]);	\
    last_val[ch][0] = left;	\
    last_val[ch][1] = right;	\
   }

#define SYNCSAMPLE_NOISE(wt)  \
   {    \
    int32_t left = sample_cache[ch][0], right = sample_cache[ch][1];      \
    NoiseSynth.offset_inline(wt, left - last_val[ch][0], &sbuf[0]);      \
    NoiseSynth.offset_inline(wt, right - last_val[ch][1], &sbuf[1]);     \
    last_val[ch][0] = left;     \
    last_val[ch][1] = right;    \
   }

void WSwan_SoundUpdate(void) {

	int32_t run_time;

	//printf("%d\n", v30mz_timestamp);
	//printf("%02x %02x\n", control, noise_control);
	run_time = v30mz_timestamp - last_ts;

	for (unsigned int ch = 0; ch < 4; ch++) {
		// Channel is disabled?
		if (!(control & (1 << ch)))
			continue;

		if (ch == 1 && (control & 0x20)) // Direct D/A mode?
				{
			int32_t neoval = (volume[ch] - 0x80) * voice_volume;

			VoiceSynth.offset(v30mz_timestamp, neoval - last_v_val, &sbuf[0]);
			VoiceSynth.offset(v30mz_timestamp, neoval - last_v_val, &sbuf[1]);

			last_v_val = neoval;
		}
		else if (ch == 2 && (control & 0x40) && sweep_value) // Sweep
				{
			uint32_t tmp_pt = 2048 - period[ch];
			uint32_t meow_timestamp = v30mz_timestamp - run_time;
			uint32_t tmp_run_time = run_time;

			while (tmp_run_time) {
				int32_t sub_run_time = tmp_run_time;

				if (sub_run_time > sweep_8192_divider)
					sub_run_time = sweep_8192_divider;

				sweep_8192_divider -= sub_run_time;
				if (sweep_8192_divider <= 0) {
					sweep_8192_divider += 8192;
					sweep_counter--;
					if (sweep_counter <= 0) {
						sweep_counter = sweep_step + 1;
						period[ch] = (period[ch] + (int8_t) sweep_value) & 0x7FF;
					}
				}

				meow_timestamp += sub_run_time;
				if (tmp_pt > 4) {
					period_counter[ch] -= sub_run_time;
					while (period_counter[ch] <= 0) {
						sample_pos[ch] = (sample_pos[ch] + 1) & 0x1F;

						MK_SAMPLE_CACHE;
						SYNCSAMPLE(meow_timestamp + period_counter[ch]);
						period_counter[ch] += tmp_pt;
					}
				}
				tmp_run_time -= sub_run_time;
			}
		}
		else if (ch == 3 && (noise_control & 0x10)) //(control & 0x80)) // Noise
				{
			uint32_t tmp_pt = 2048 - period[ch];

			period_counter[ch] -= run_time;
			while (period_counter[ch] <= 0) {
				// Yay, random numbers, so let's use totally wrong numbers to make them!
				const int bstab1[8] = { 14, 13, 12, 14, 12, 13, 14, 14 };
				const int bstab2[8] = { 13, 12, 9, 12, 1, 1, 5, 11 };

				nreg = (~((nreg << 1)
						| (((nreg >> bstab1[noise_control & 0x7]) & 1) ^ ((nreg >> bstab2[noise_control & 0x7]) & 1))))
						& 0x7FFF;
				if (control & 0x80) {
					MK_SAMPLE_CACHE_NOISE;
					SYNCSAMPLE_NOISE(v30mz_timestamp + period_counter[ch]);
				}
				else if (tmp_pt > 4) {
					sample_pos[ch] = (sample_pos[ch] + 1) & 0x1F;
					MK_SAMPLE_CACHE;
					SYNCSAMPLE(v30mz_timestamp + period_counter[ch]);
				}
				period_counter[ch] += tmp_pt;
			}
		}
		else {
			uint32_t tmp_pt = 2048 - period[ch];

			if (tmp_pt > 4) {
				period_counter[ch] -= run_time;
				while (period_counter[ch] <= 0) {
					sample_pos[ch] = (sample_pos[ch] + 1) & 0x1F;

					MK_SAMPLE_CACHE;
					SYNCSAMPLE(v30mz_timestamp + period_counter[ch]);
					// - period_counter[ch]);
					period_counter[ch] += tmp_pt;
				}
			}
		}
	}

	{
		int32_t tmphv = HyperVoice;

		if (tmphv - last_hv_val) {
			WaveSynth.offset_inline(v30mz_timestamp, tmphv - last_hv_val, &sbuf[0]);
			WaveSynth.offset_inline(v30mz_timestamp, tmphv - last_hv_val, &sbuf[1]);
			last_hv_val = tmphv;
		}
	}
	last_ts = v30mz_timestamp;
}

extern "C" void wswan_soundwrite(uint32_t A, uint8_t V) {
	WSwan_SoundWrite(A, V);
}

void WSwan_SoundWrite(uint32_t A, uint8_t V) {

	//LOGD("WSwan_SoundWrite");

	WSwan_SoundUpdate();

	if (A >= 0x80 && A <= 0x87) {
		int ch = (A - 0x80) >> 1;

		if (A & 1)
			period[ch] = (period[ch] & 0x00FF) | ((V & 0x07) << 8);
		else
			period[ch] = (period[ch] & 0x0700) | ((V & 0xFF) << 0);
	}
	else if (A >= 0x88 && A <= 0x8B) {
		volume[A - 0x88] = V;
	}
	else if (A == 0x8C)
		sweep_value = V;
	else if (A == 0x8D) {
		sweep_step = V;
		sweep_counter = sweep_step + 1;
		sweep_8192_divider = 8192;
	}
	else if (A == 0x8E) {
		noise_control = V;
		if (V & 0x8)
			nreg = 1;
		//printf("NOISECONTROL: %02x\n", V);
	}
	else if (A == 0x90) {
		for (int n = 0; n < 4; n++)
			if (!(control & (1 << n)) && (V & (1 << n))) {
				period_counter[n] = 0;
				sample_pos[n] = 0x1F;
			}
		control = V;
		//printf("Sound Control: %02x\n", V);
	}
	else if (A == 0x91) {
		output_control = V & 0xF;
		//printf("%02x, %02x\n", V, (V >> 1) & 3);
	}
	else if (A == 0x92)
		nreg = (nreg & 0xFF00) | (V << 0);
	else if (A == 0x93)
		nreg = (nreg & 0x00FF) | ((V & 0x7F) << 8);
	else if (A == 0x94) {
		voice_volume = V & 0xF;
		//printf("%02x\n", V);
	}
	else
		switch (A) {
			case 0x8F:
				SampleRAMPos = V;
				break;
			case 0x95:
				HyperVoice = V;
				break; // Pick a port, any port?!
				//default: printf("%04x:%02x\n", A, V); break;
		}
	WSwan_SoundUpdate();
}

extern "C" uint8_t wswan_soundread(uint32_t A) {
	return WSwan_SoundRead(A);
}

uint8_t WSwan_SoundRead(uint32_t A) {

	//LOGD("WSwan_SoundRead");

	WSwan_SoundUpdate();

	if (A >= 0x80 && A <= 0x87) {
		int ch = (A - 0x80) >> 1;

		if (A & 1)
			return (period[ch] >> 8);
		else
			return (period[ch]);
	}
	else if (A >= 0x88 && A <= 0x8B)
		return (volume[A - 0x88]);
	else
		switch (A) {
			default:
				/*printf("SoundRead: %04x\n", A); */
				return (0);
			case 0x8C:
				return (sweep_value);
			case 0x8D:
				return (sweep_step);
			case 0x8E:
				return (noise_control);
			case 0x8F:
				return (SampleRAMPos);
			case 0x90:
				return (control);
			case 0x91:
				return (output_control | 0x80);
			case 0x92:
				return ((nreg >> 0) & 0xFF);
			case 0x93:
				return ((nreg >> 8) & 0xFF);
			case 0x94:
				return (voice_volume);
		}
}

extern "C" int16_t wswan_soundflush(int16_t *buffer) {
	return WSwan_SoundFlush(buffer);
}

int16_t WSwan_SoundFlush(int16_t *buffer) {

	//LOGD("> WSwan_SoundFlush()");

	WSwan_SoundUpdate();

	int love;
	for (int y = 0; y < 2; y++) // 2
			{
		sbuf[y].end_frame(v30mz_timestamp);
		love = sbuf[y].read_samples((buffer + y), uk_org_cardboardbox_wonderdroid_WonderSwan_audiobufferlen, 1);
	}

	last_ts = 0;
	v30mz_timestamp = 0;

	//LOGD("< WSwan_SoundFlush()");

	return love;
}

extern "C" void wswan_soundclear() {
	return WSwan_SoundClear();
}

void WSwan_SoundClear() {

	for (int y = 0; y < 2; y++) // 2
			{
		sbuf[y].clear(1);
	}
	last_ts = 0;
	v30mz_timestamp = 0;
}

extern "C" void wswan_soundcheckramwrite(uint32_t A) {
	WSwan_SoundCheckRAMWrite(A);
}

// Call before wsRAM is updated
void WSwan_SoundCheckRAMWrite(uint32_t A) {
	if ((A >> 6) == SampleRAMPos)
		WSwan_SoundUpdate();
}

static void RedoVolume(void) {
	WaveSynth.volume((double) .50);
	NoiseSynth.volume((double) .50);
	VoiceSynth.volume((double) .50);
}

extern "C" void wswan_soundinit(void) {
	WSwan_SoundInit();
}

void WSwan_SoundInit() {
	for (int i = 0; i < 2; i++) {
		sbuf[i].set_sample_rate(uk_org_cardboardbox_wonderdroid_WonderSwan_audiofreq, 60);
		sbuf[i].clock_rate((long) (3072000));
		sbuf[i].bass_freq(20);
	}

	RedoVolume();
}

void WSwan_Sound(int rate) {
	for (int i = 0; i < 2; i++)
		sbuf[i].set_sample_rate(rate ? rate : uk_org_cardboardbox_wonderdroid_WonderSwan_audiofreq, 10);
}

extern "C" void wswan_soundreset(void) {
	WSwan_SoundReset();
}

void WSwan_SoundReset(void) {
	memset(period, 0, sizeof(period));
	memset(volume, 0, sizeof(volume));
	voice_volume = 0;
	sweep_step = 0;
	sweep_value = 0;
	noise_control = 0;
	control = 0;
	output_control = 0;

	sweep_8192_divider = 8192;
	sweep_counter = 0;
	SampleRAMPos = 0;
	memset(period_counter, 0, sizeof(period_counter));
	memset(sample_pos, 0, sizeof(sample_pos));
	nreg = 1;

	memset(sample_cache, 0, sizeof(sample_cache));
	memset(last_val, 0, sizeof(last_val));
	last_v_val = 0;

	HyperVoice = 0;
	last_hv_val = 0;

	sbuf[0].clear();
	sbuf[1].clear();

}
