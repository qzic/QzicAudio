package ca.qzic.qzicaudio;
import javax.sound.sampled.AudioInputStream;
import java.io.*;
import javax.sound.sampled.AudioSystem;


public class AudioInfo2 {
	    private static final int NUM_BITS_PER_BYTE = 8;

	    private AudioInputStream ais;
	    private double[][] samplesContainer;
	    private double[][] staContainer;
	    private double[] shifted;
	    private double[] scaled;

	    //cached values
	    protected double sampleMax = 0;
	    protected double sampleMin = 0;
	    protected double biggestSample = 0;
	    protected int numChannels;
	    protected double sampleRate;
	    protected int bytesPerSample;
	    
		static double ki = Math.pow(2, -7)/6.0;
		
		public void setSTA_TimeConstant(double STA_TimeConstant){
			ki = STA_TimeConstant;
		}
		
		public double getSTA_TimeConstant(){
			return ki;
		}

	    public AudioInfo2(AudioInputStream aiStream) {
	        this.ais = aiStream;
	        bytesPerSample = ais.getFormat().getSampleSizeInBits() / NUM_BITS_PER_BYTE;
	        numChannels = ais.getFormat().getFrameSize() / bytesPerSample;
	        sampleRate = ais.getFormat().getSampleRate();
	        createSampleArrayCollection();
	    }

	    public AudioInfo2(String audioFileName) {
	    	try {
		        this.ais = AudioSystem.getAudioInputStream(new File(audioFileName));
		        bytesPerSample = ais.getFormat().getSampleSizeInBits() / NUM_BITS_PER_BYTE;
		        numChannels = ais.getFormat().getFrameSize() / bytesPerSample;
		        sampleRate = ais.getFormat().getSampleRate();
		        createSampleArrayCollection();
			} catch (Exception e){
				e.printStackTrace();
			}
	    }
	    
		public AudioInfo2(double data[],int blockSize) {
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
	                biggestSample = Math.abs(sampleMin);
	            }
	    	}
	    	// Kludge - sampleRate is actually unknown
	        sampleRate = 48000;
	    	// System.out.println("Biggest sample = " + biggestSample);
	    }
	    
	    public int getNumberOfChannels(){
	        return numChannels;
	    }
	    
	    
	    public int getSampleRate(){
	        return (int) Math.ceil(sampleRate);
	    }

	    public int getBytesPerSample(){
	        return bytesPerSample;
	    }

	    public void normalize() {
	    	for (int i=0; i<samplesContainer[0].length; i++) {
	    		for (int a = 0; a < getNumberOfChannels(); a++) {
	    			samplesContainer[a][i] = samplesContainer[a][i]/biggestSample;
	    		}
	    	}
	    }
	    
	    public double[] scale(double k) {
	    	scaled= new double[samplesContainer[0].length];
	    	for (int i=0; i<samplesContainer[0].length; i++) {
	    		for (int a = 0; a < getNumberOfChannels(); a++) {
	    			scaled[i] = samplesContainer[a][i]*k;
	    		}
	    	}
	    	return scaled;
	    }
	    

	    public double[][] scale(double[][] scaled,double k) {
	    	for (int i=0; i<samplesContainer[0].length; i++) {
	    		for (int a = 0; a < getNumberOfChannels(); a++) {
	    			scaled[a][i] = samplesContainer[a][i]*k;
	    		}
	    	}
	    	return scaled;
	    }
	    
	    public double[] shift(int ms) {
	    	int numSamples = (int) (sampleRate/1000 * ms); 
	    	// System.out.printf("numsamples = %d\n",numSamples);
	    	shifted = new double[samplesContainer[0].length];
	    	for (int i=0; i<numSamples; i++) shifted[i] = 0;
	    	System.arraycopy(samplesContainer[0], 0, shifted, numSamples, samplesContainer[0].length-numSamples);
	    	return shifted;
	    }
	    
	    private void createSampleArrayCollection() {
	        try {
	        	//DEBUG
	        	//System.out.println(audioInputStream.markSupported());
	        	//End DEBUG
	        	
	            //audioInputStream.mark(Integer.MAX_VALUE);
	            //audioInputStream.reset();
	            byte[] bytes = new byte[(int) (ais.getFrameLength()) * ((int) ais.getFormat().getFrameSize())]; 
	            try {
	                ais.read(bytes);
	            } catch (Exception e) {
	                e.printStackTrace();
	            }

	            //convert sample bytes to channel separated double samples
	            samplesContainer = getSampleArray(bytes);

	            //find biggest sample. used for interpolating the yScaleFactor
	            if (sampleMax > sampleMin) {
	                biggestSample = sampleMax;
	            } else {
	                biggestSample = Math.abs(sampleMin);
	            }
	        } catch (Exception e) {
	            e.printStackTrace();
	        }
	    }

	    protected double[][] getSampleArray(byte[] eightBitByteArray) {
	        double[][] toReturn = new double[getNumberOfChannels()][eightBitByteArray.length / (bytesPerSample * getNumberOfChannels())];
	        int index = 0;
	    	int sample=0;
	    	double dSample;
	    	
	        //loop through the byte[]
	        for (int t = 0; t < eightBitByteArray.length;) {
	            //for each iteration, loop through the channels
	            for (int a = 0; a < getNumberOfChannels(); a++) {
	            	
	            	//DEBUG
	            	// System.out.println(getNumberOfChannels());
	            	//End DEBUG
	                //do the byte to sample conversion
	                //see AmplitudeEditor for more info
	            	
	                if(bytesPerSample == 2) {
	        			sample = (int) (eightBitByteArray[t + 1]) << 24;
	        			sample |= (int) (eightBitByteArray[t + 0]) << 16 & 0xFF0000;
		                t +=2;
	                }
	                if(bytesPerSample == 3) {
	        			sample = (int) (eightBitByteArray[t + 2]) << 24;
	        			sample |= (int) (eightBitByteArray[t + 1]) << 16 & 0xFF0000;
	        			sample |= (int) (eightBitByteArray[t + 0]) << 8 & 0xFF00;
	        			t += 3;
	                }
	                dSample = (double) sample / Integer.MAX_VALUE;
		            if (dSample < sampleMin) {
		                sampleMin = dSample;
		            } else if (dSample > sampleMax) {
		                sampleMax = dSample;
		            }
	            
	            //set the value.
	            toReturn[a][index] = dSample;
	            }
	            index++;
	        }

	        return toReturn;
	    }

	    public double getXScaleFactor(int panelWidth){
	    	double retval=1;
	    	if(staContainer != null)
	    		retval =  (panelWidth / ((double) staContainer[0].length));
	    	else if(samplesContainer != null)
	    		retval = (panelWidth / ((double) samplesContainer[0].length));
	    	return retval;
	    }

	    public double getYScaleFactor(int panelHeight){
	        return (panelHeight / (biggestSample * 2));
	    }

	    public double[] getAudio(int channel){
	        return samplesContainer[channel];
	    }
	    
	    public double[] getAudio(){
	        return samplesContainer[0];
	    }
	    
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
					staContainer[chn][staIndex] = Math.abs(samplesContainer[chn][i] * ki) + staContainer[chn][staIndex] * a1;
					if(i>0 && i % samplesPerBlock == 0) {
						// System.out.println(staContainer[chn][staIndex]);
						staIndex++;
					}
				}
			}
			for(int chn=0; chn<numChannels; chn++) {
				for (int i=0; i< staContainer[chn].length; i++) {
					staContainer[chn][i] = 20.0*Math.log10(staContainer[chn][i]);
				}
				
			}
			return staContainer[0];
		}

	    protected int getIncrement(double xScale) {
	        try {
	            int increment = (int) (samplesContainer[0].length / (samplesContainer[0].length * xScale));
	            return increment;
	        } catch (Exception e) {
	            e.printStackTrace();
	        }
	        return -1;
	    }

	}



