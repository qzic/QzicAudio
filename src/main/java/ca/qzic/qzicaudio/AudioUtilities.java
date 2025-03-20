package ca.qzic.qzicaudio;

import javax.sound.sampled.*;
import static java.lang.Math.*;

//import com.sun.xml.internal.ws.client.PortInfo;

public class AudioUtilities {
	static public double ki = pow(2, -7)/6.0;
	
	static public void setSTA_TimeConstant(double STA_TimeConstant){
		ki = STA_TimeConstant;
	}
	
	static public double getSTA_TimeConstant(){
		return ki;
	}
	
	// convert a double vector to 16 bits (2-bytes)
	static public double doubleToByte2(double[] doubleSamples, byte[] byteSamples, double sta) {
		short sample;
		for (int cnt = 0; cnt < doubleSamples.length; cnt++) {
			sample = (short) (doubleSamples[cnt] * Short.MAX_VALUE);
			sta = xx_STA(doubleSamples[cnt], sta);
			byteSamples[cnt * 2 + 0] = (byte) (sample & 0xFF);
			byteSamples[cnt * 2 + 1] = (byte) ((sample >> 8) & 0xFF);
		}
		return sta;
	}

	// convert a double vector to 16 bits (2-bytes)
	static public void doubleToByteTwo(double[] doubleSamples, byte[] byteSamples) {
		short sample;
		for (int cnt = 0; cnt < doubleSamples.length; cnt++) {
			sample = (short) (doubleSamples[cnt] * Short.MAX_VALUE);
			byteSamples[cnt * 2 + 0] = (byte) (sample & 0xFF);
			byteSamples[cnt * 2 + 1] = (byte) ((sample >> 8) & 0xFF);
		}
	}
	
	// convert a double vector to 24 bits (3-bytes)
	static public double doubleToByte3(double[] doubleSamples, byte[] byteSamples, double sta) {
		int sample;
		for (int cnt = 0; cnt < doubleSamples.length; cnt++) {
			sample = (int) (doubleSamples[cnt] * Integer.MAX_VALUE);
			sta = xx_STA(doubleSamples[cnt], sta);
			byteSamples[cnt * 3 + 0] = (byte) ((sample >> 8) & 0xFF);
			byteSamples[cnt * 3 + 1] = (byte) ((sample >> 16) & 0xFF);
			byteSamples[cnt * 3 + 2] = (byte) ((sample >> 24) & 0xFF);
		}
		return sta;
	}
	// convert a double vector to 24 bits (3-bytes)
	static public void doubleToByteThree(double[] doubleSamples, byte[] byteSamples) {
		int sample;
		for (int cnt = 0; cnt < doubleSamples.length; cnt++) {
			sample = (int) (doubleSamples[cnt] * Integer.MAX_VALUE);
			byteSamples[cnt * 3 + 0] = (byte) ((sample >> 8) & 0xFF);
			byteSamples[cnt * 3 + 1] = (byte) ((sample >> 16) & 0xFF);
			byteSamples[cnt * 3 + 2] = (byte) ((sample >> 24) & 0xFF);
		}
	}

	// convert a 16 bit (2-bytes) vector to a double vector
	static public double byte2ToDouble(byte[] byteSamples, double[] doubleSamples, double sta) {
		short sample;
		for (int cnt = 0; cnt < doubleSamples.length; cnt++) {
			sample = (short) (byteSamples[cnt * 2 + 1] << 8);
			sample |= (short) (byteSamples[cnt * 2 + 0]) & 0xFF;
			doubleSamples[cnt] = (double) sample / Short.MAX_VALUE;
			sta = xx_STA(doubleSamples[cnt], sta);
		}
		return sta;
	}

	// convert a 24 bit (3-bytes) vector to a double vector
	static public double byte3ToDouble(byte[] byteSamples, double[] doubleSamples, double sta) {
		int sample;
		for (int cnt = 0; cnt < doubleSamples.length; cnt++) {
			sample = (int) (byteSamples[cnt * 3 + 2]) << 24;
			sample |= (int) (byteSamples[cnt * 3 + 1]) << 16 & 0xFF0000;
			sample |= (int) (byteSamples[cnt * 3 + 0]) << 8 & 0xFF00;
			doubleSamples[cnt] = (double) sample / Integer.MAX_VALUE;
			sta = xx_STA(doubleSamples[cnt], sta);
		}
		return sta;
	}

	static public double xx_LTA(double in, double out) {
		final double ki = pow(2, -7) / 6.0;
		final double a1 = 1 - ki;
		out = abs(in * ki) + out * a1;
		if (out > 1.0)
			out = 1.0;
		return out;
	}

	static public double xx_STA(double in, double out) {
		final double ki = pow(2, -7)/6.0;
		// final double ki = 0.02;
		final double a1 = 1 - ki;
		out = abs(in * ki) + out * a1;
		if (out > 1.0)
			out = 1.0;
		return out;
	}

	static public double xx_MIN(double in, double out) {
		if (in < 0.0001)
			in = 0.0001; // limit to -80dB FS
		if (in < out) {
			out = in;
		} else {
			out = out * 1.005; // about 2dB/sec rise
		}
		return out;
	}

	static public double xx_MIN(double[] in) {
		double out = in[0];
		for(int i=1; i<in.length; i++) {
			if (in[i] < out) {
				out = in[i];
			} 
		}
		return out;
	}
	static public double xx_MAX(double in, double out) {
		if (in > out) {
			out = in;
		} else {
			out = out / 1.0001;
		}
		if (out > 1.0)
			out = 1.0;
		return out;
	}
	
	static public double xx_STA(double in[], int offset, int numSamples) {
		double out = 0.0;
		for(int i= 0; i< numSamples; i++) {
			out += abs(in[offset+i]);
		}
		return out/numSamples;
	}	
	
	static public double[] xx_STA(double in[], double out[], int samplesPerBlock) {
		
		// System.out.println(sampleRate + ", " + blockSize + ", " + samplesPerBlock);
		out = new double[(int) floor(in.length/samplesPerBlock)+1];
		// final double ki = 0.02;
		double a1 = 1 - ki;
		int staIndex = 0;
		for (int i=0; i < out.length; i++) out[i] = 0;
		for (int i=0; i< in.length-1; i++) {
			// System.out.println(staContainer[chn].length + ", " + i + ", " + staContainer[chn].length + ", " + staIndex);
			// System.out.println(i + ", " + staIndex);
			out[staIndex] = abs(in[i] * ki) + out[staIndex] * a1;
			if(i>0 && i % samplesPerBlock == 0) {
				// System.out.println(staContainer[chn][staIndex]);
				staIndex++;
			}
		}
		for (int i=0; i<out.length; i++) {
			if (out[i] != 0) {
			 out[i] = 20.0*log10(out[i]);
			}
			else {
			 out[i] = -144.0;
			}
		}
		return out;
	}
	
	static public double[] xx_LTA(double in[], double out[], int samplesPerBlock) {
		// System.out.println(sampleRate + ", " + blockSize + ", " + samplesPerBlock);
		out = new double[(int) floor(in.length/samplesPerBlock)+1];
		double ltaScale = 0.01;
		double a1 = 1 - ki*ltaScale;
		int staIndex = 0;
		for (int i=0; i < out.length; i++) out[i] = 0;
		for (int i=0; i< in.length-1; i++) {
			// System.out.println(staContainer[chn].length + ", " + i + ", " + staContainer[chn].length + ", " + staIndex);
			// System.out.println(i + ", " + staIndex);
			out[staIndex] = abs(in[i] * ki*ltaScale) + out[staIndex] * a1;
			if(i>0 && i % samplesPerBlock == 0) {
				// System.out.println(staContainer[chn][staIndex]);
				staIndex++;
			}
		}
		for (int i=0; i<out.length; i++) {
			if (out[i] != 0) {
			 out[i] = 20.0*log10(out[i]);
			}
			else {
			 out[i] = -144.0;
			}
		}
		return out;
	}
	
	static public double avgPower(double[][] in) {
		double p = 0;
		for(int i=1; i<in.length; i++) {
			p += in[i][0]*in[i][0];
		}
		return p/in.length;
	}
	
	static public double Max(double[] in) {
		double max = 0;
		for(int i=1; i<in.length; i++) {
			if (abs(in[i]) > max) max = in[i];
		}
		return max;
	}
	
	static public double Min(double[] in) {
		double min = 1e99;
		for(int i=1; i<in.length; i++) {
			if (abs(in[i]) < min) min = in[i];
		}
		return min;
	}
	
	static public double[] normalize(double in[]) {
		double biggestSample = 0;
    	for (int i=0; i<in.length; i++) {
    		if(abs(in[i]) > biggestSample) {
    			biggestSample = abs(in[i]);
    		}
    	}
    	for (int i=0; i<in.length; i++) {
    		in[i] = in[i]/biggestSample;
    	}
    	return in;
    }
	
	static public double[] scale(double in[], double scale) {
    	for (int i=0; i<in.length; i++) {
    		in[i] *= scale;
    	}
    	return in;
    }
	
	static public void ShowMixers() {
		Mixer.Info mixerInfo[] = AudioSystem.getMixerInfo();
		int AudioFire = -1;
		
		for(int i = 0; i < mixerInfo.length; i++)
		{
		     // System.out.println(mixerInfo[i].getName());
		     if(mixerInfo[i].getName().contentEquals("Analog out 1-2 (AudioFire 8a)")) {
		    	 AudioFire = i;
		     }
		}
		
		System.out.println(mixerInfo[AudioFire].getName());
		System.out.println(mixerInfo[AudioFire].getVendor());
		System.out.println(mixerInfo[AudioFire].getVersion());
		System.out.println(mixerInfo[AudioFire].getDescription());
		
		Mixer mixer = AudioSystem.getMixer(mixerInfo[AudioFire]);
		Line.Info lines[] = mixer.getSourceLineInfo();
		System.out.println("# lines: " + lines.length);
		
		for(int i = 0; i < lines.length; i++){
			try {
				System.out.println(mixer.getLine(lines[i]));
			} catch (LineUnavailableException e) {
				e.printStackTrace();
			}
		}
	}
	
	static public void DisplayAudioFormat(String prefix,AudioFormat audioFormat){
		System.out.println(prefix + "Encoding - " + audioFormat.getEncoding());
		System.out.println(prefix + "Sample Rate - " + audioFormat.getSampleRate());
	    System.out.println(prefix + "Frame Rate - " + audioFormat.getFrameRate());	
	    System.out.println(prefix + "Bytes per Frame - " + audioFormat.getFrameSize());
	    System.out.println(prefix + "Bits per Sample - " + audioFormat.getSampleSizeInBits());
	    System.out.println(prefix + "Channesl per frame - " + audioFormat.getChannels());
		System.out.println(prefix + "bigEndian - " + audioFormat.isBigEndian());
	}
	
    public static void main(String[] args) 
    { 
    	ShowMixers();
    }
    

}
