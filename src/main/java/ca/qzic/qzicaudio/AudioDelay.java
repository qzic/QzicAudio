package ca.qzic.qzicaudio;

// implements a circular buffer
public class AudioDelay {
	static int MAX_DELAY = 48000;	// default 1 sec circular buffer
	int curIptr = 0;
	int curOptr = 0;
	double delayBuffer[];
	int bufferSize;
	
	
	public AudioDelay(int maxDelay) {
		delayBuffer = new double[maxDelay];
		bufferSize = maxDelay;
	}
	public AudioDelay() {
		delayBuffer = new double[MAX_DELAY];
		bufferSize = MAX_DELAY;
	}
	
	public void process(double[] input, double[] output, int delay){
		
		for(int i=0; i<input.length; i++) {
			delayBuffer[curIptr] = input[i];
			output[i] = delayBuffer[curOptr];
			curIptr--;
			if(curIptr<0) curIptr = bufferSize-1;
			curOptr = (curIptr+delay)%bufferSize;
		}
	}
}
