package ca.qzic.qzicaudio;

import javax.sound.sampled.AudioInputStream;
import java.io.*;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;

import javax.sound.sampled.AudioSystem;
import static java.lang.Math.*;
import static java.lang.System.out;

public class AudioInfo3 {
	    private static final int NUM_BITS_PER_BYTE = 8;

	    private AudioInputStream ais;
	    private double[][] samplesContainer;
	    private double[][] staContainer;
	    private double[] shifted;
	    private double[] scaled;

	    //cached values
	    private double sampleMax = 0;
	    private double sampleMin = 0;
	    private double sampleAvg[];				// average signal level within top 30dB
	    protected double biggestSample = 0;
	    protected int numChannels;
	    protected double sampleRate;
	    protected int bytesPerSample;
	    protected long numSamplesFile;
	    protected ByteOrder endian;
	    
		static double ki = pow(2, -7)/6.0;
		
		// ---------------------------------------------------------------------------------------------
	    public AudioInfo3(AudioInputStream aiStream) {
	        this.ais = aiStream;
	        bytesPerSample = ais.getFormat().getSampleSizeInBits() / NUM_BITS_PER_BYTE;
	        numChannels = ais.getFormat().getFrameSize() / bytesPerSample;
	        sampleRate = ais.getFormat().getSampleRate();
	        numSamplesFile = ais.getFrameLength();
	        endian = ais.getFormat().isBigEndian() ? ByteOrder.BIG_ENDIAN : ByteOrder.LITTLE_ENDIAN;
	        sampleAvg = new double[1];
	        createSampleArrayCollection();
	    }

		// ---------------------------------------------------------------------------------------------
	    public AudioInfo3(String audioFileName) {
	    	try {
		        this.ais = AudioSystem.getAudioInputStream(new File(audioFileName));
		        // out.println("Audio Format: " + ais.getFormat());
		        bytesPerSample = ais.getFormat().getSampleSizeInBits() / NUM_BITS_PER_BYTE;
		        numChannels = ais.getFormat().getFrameSize() / bytesPerSample;
		        sampleRate = ais.getFormat().getSampleRate();
		        numSamplesFile = ais.getFrameLength();
		        endian = ais.getFormat().isBigEndian() ? ByteOrder.BIG_ENDIAN : ByteOrder.LITTLE_ENDIAN;
		        sampleAvg = new double[1];
		        createSampleArrayCollection();
	            // out.println("sample array size = " + samplesContainer[0].length);
			} catch (Exception e){
				e.printStackTrace();
			}
	    }
	    
		// ---------------------------------------------------------------------------------------------
		public AudioInfo3(double data[],int blockSize) {
			numChannels = 1;
			samplesContainer = new double [1][data.length];
	    	for (int i=0; i<data.length; i++) {
	    		samplesContainer[0][i] = data[i];
	            if (data[i] < sampleMin) {
	                sampleMin = data[i];
	            } else if (data[i] > sampleMax) {
	                sampleMax = data[i];
	            }
	            if (sampleMax > sampleMin) {
	                biggestSample = sampleMax;
	            } else {
	                biggestSample = abs(sampleMin);
	            }
	    	}
	    	// Kludge - sampleRate is actually unknown
	        sampleRate = 48000;
	    	// System.out.println("Biggest sample = " + biggestSample);
	        sampleAvg = new double[1];
	    }
		
		// ---------------------------------------------------------------------------------------------
		public void setSTA_TimeConstant(double STA_TimeConstant){
			ki = STA_TimeConstant;
		}
		
		// ---------------------------------------------------------------------------------------------
		public double getSTA_TimeConstant(){
			return ki;
		}
	    
		// ---------------------------------------------------------------------------------------------
	    public int getNumberOfChannels(){
	        return numChannels;
	    }
	    
		// ---------------------------------------------------------------------------------------------
	    public int getSampleRate(){
	        return (int) ceil(sampleRate);
	    }

		// ---------------------------------------------------------------------------------------------
	    public int getBytesPerSample(){
	        return bytesPerSample;
	    }

		// ---------------------------------------------------------------------------------------------
	    public double[] getAudio(int channel){
	        return samplesContainer[channel];
	    }
	    
		// ---------------------------------------------------------------------------------------------
	    public double[] getAudio(){
	        return samplesContainer[0];
	    }
	    
	    public double getSampleMax(){
	        return 20 * log10(biggestSample);
	    }
	    
	    public double getSampleAvg(){
	        return 20 * log10(sampleAvg[0]);
	    }
	    
		// ---------------------------------------------------------------------------------------------
		public double[] xx_STA(int blockSize) {
			int samplesPerBlock = (int) sampleRate/1000*blockSize;
			// System.out.println(sampleRate + ", " + blockSize + ", " + samplesPerBlock);
			staContainer = new double [numChannels][samplesContainer[0].length/samplesPerBlock +1];
			// final double ki = 0.02;
			double a1 = 1 - ki;
			int staIndex = 0;
			for(int chn=0; chn<numChannels; chn++) {
				for (int i=0; i< samplesContainer[chn].length; i++) {
					// System.out.println(samplesContainer[chn].length + ", " + i + ", " + staContainer[chn].length + ", " + staIndex);
					staContainer[chn][staIndex] = abs(samplesContainer[chn][i] * ki) + staContainer[chn][staIndex] * a1;
					if(i>0 && i % samplesPerBlock == 0) {
						// System.out.println(staContainer[chn][staIndex]);
						staIndex++;
					}
				}
			}
			for(int chn=0; chn<numChannels; chn++) {
				for (int i=0; i< staContainer[chn].length; i++) {
					staContainer[chn][i] = 20.0*log10(staContainer[chn][i]);
				}
				
			}
			return staContainer[0];
		}

		// ---------------------------------------------------------------------------------------------
	    public double getXScaleFactor(int panelWidth){
	    	double retval=1;
	    	if(staContainer != null)
	    		retval =  (panelWidth / ((double) staContainer[0].length));
	    	else if(samplesContainer != null)
	    		retval = (panelWidth / ((double) samplesContainer[0].length));
	    	return retval;
	    }
	    
		// ---------------------------------------------------------------------------------------------
	    public double getYScaleFactor(int panelHeight){
	        return (panelHeight / (biggestSample * 2));
	    }
	    
		// ---------------------------------------------------------------------------------------------
	    protected int getIncrement(double xScale) {
	        try {
	            int increment = (int) (samplesContainer[0].length / (samplesContainer[0].length * xScale));
	            return increment;
	        } catch (Exception e) {
	            e.printStackTrace();
	        }
	        return -1;
	    }
	    
		// ---------------------------------------------------------------------------------------------
	    public void normalize() {
	    	for (int i=0; i<samplesContainer[0].length; i++) {
	    		for (int a = 0; a < getNumberOfChannels(); a++) {
	    			samplesContainer[a][i] = samplesContainer[a][i]/biggestSample;
	    		}
	    	}
	    }
	    
		// ---------------------------------------------------------------------------------------------
	    public double[] scale(double k) {
	    	scaled= new double[samplesContainer[0].length];
	    	for (int i=0; i<samplesContainer[0].length; i++) {
	    		for (int a = 0; a < getNumberOfChannels(); a++) {
	    			scaled[i] = samplesContainer[a][i]*k;
	    		}
	    	}
	    	return scaled;
	    }
	    
		// ---------------------------------------------------------------------------------------------
	    public double[][] scale(double[][] scaled,double k) {
	    	for (int i=0; i<samplesContainer[0].length; i++) {
	    		for (int a = 0; a < getNumberOfChannels(); a++) {
	    			scaled[a][i] = samplesContainer[a][i]*k;
	    		}
	    	}
	    	return scaled;
	    }
	    
		// ---------------------------------------------------------------------------------------------
	    public double[] shift(int ms) {
	    	shifted = null;
	    	int numSamples = (int) (sampleRate/1000 * ms); 
	    	// System.out.printf("numsamples = %d\n",numSamples);
	    	shifted = new double[samplesContainer[0].length];
	    	for (int i=0; i<numSamples; i++) shifted[i] = 0;
	    	System.arraycopy(samplesContainer[0], 0, shifted, numSamples, samplesContainer[0].length-numSamples);
	    	return shifted;
	    }
	    
	    
	    private void analyseSamplesContainer() {
            // find max min and avg
            for(int i=0; i < getNumberOfChannels() ; i++) {
            	 sampleAvg[i] =0;
            	int length = samplesContainer[i].length;
            	for (int j=0; j<length; j++) {
            		double sample = abs(samplesContainer[i][j]);
                    if (sample < sampleMin) {
                        sampleMin = sample;
                    } else if (sample > sampleMax) {
                        sampleMax = sample;
                    }
            		if(sample > pow(10, (getSampleMax()-30)/20)) sampleAvg[i] += sample;
            	}
	            sampleAvg[i] /= length;
            }
            
            //find biggest sample. used for interpolating the yScaleFactor
            if (sampleMax > sampleMin) {
                biggestSample = sampleMax;
            } else {
                biggestSample = abs(sampleMin);
            }
	    }
	    
		// ---------------------------------------------------------------------------------------------
	    private void createSampleArrayCollection() {
	        try {
	            byte[] bytes;
	            if(bytesPerSample == 4){
	            	bytes = new byte[(int)numSamplesFile];  
	            } else {
	            	bytes = new byte[(int)numSamplesFile * bytesPerSample]; 
	            }
	            try {
	                ais.read(bytes);
	            } catch (Exception e) {
	                e.printStackTrace();
	            }
	            // out.println(" fl " + ais.getFrameLength() + ", fs " + ais.getFormat().getFrameSize());
	            // out.println("byte array size = " + bytes.length);
	            //convert sample bytes to channel separated double samples
	            samplesContainer = getSampleArray(bytes);
	            analyseSamplesContainer();

	            
	        } catch (Exception e) {
	            e.printStackTrace();
	        }
	    }

		// ---------------------------------------------------------------------------------------------
	    protected double[][] getSampleArray(byte[] eightBitByteArray) {
	        double[][] toReturn = new double[getNumberOfChannels()][eightBitByteArray.length / (bytesPerSample * getNumberOfChannels())];
	        int index = 0;
	    	int sample=0;
	    	double dSample=0;
	    	
	    	ByteBuffer b = ByteBuffer.wrap(eightBitByteArray);
	    	// b.clear();
	    	b.order(endian);

	        //loop through the byte[]
	        for (int t = 0; t < eightBitByteArray.length;) {
	            //for each iteration, loop through the channels
	            for (int a = 0; a < getNumberOfChannels(); a++) {
	            	
	                if(bytesPerSample == 2) {
		                dSample = (double) b.getShort() / Short.MAX_VALUE;
		                t +=2;
	                }
	                
/*	                if(bytesPerSample == 2) {
	        			sample = (int) (eightBitByteArray[t + 1]) << 24;
	        			sample |= (int) (eightBitByteArray[t + 0]) << 16 & 0xFF0000;
		                t +=2;
		                dSample = (double) sample / Integer.MAX_VALUE;
	                }*/
	                
	                if(bytesPerSample == 3) {
	        			sample = (int) (eightBitByteArray[t + 2]) << 24;
	        			sample |= (int) (eightBitByteArray[t + 1]) << 16 & 0xFF0000;
	        			sample |= (int) (eightBitByteArray[t + 0]) << 8 & 0xFF00;
	        			t += 3;
		                dSample = (double) sample / Integer.MAX_VALUE;
	                }
	                
	                if(bytesPerSample == 4) {
		                dSample = (double) b.getFloat();
	        			t += 4;
	                }
	            
	            //set the value.
	            toReturn[a][index] = dSample;
	            }
	            index++;
	        }
	        
	        return toReturn;
	    }

	}



