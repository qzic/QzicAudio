package ca.qzic.qzicaudio;

public class StaInfo {
	    private double[] staContainer;

	    //cached values
	    protected double biggestSample = 0;
	    
		public StaInfo(double data[]) {
			staContainer = new double[data.length];
	    	for (int i=0; i<data.length; i++) {
	    		staContainer[i] = data[i];
	    		double d = Math.abs(data[i]);
	    		if (d > biggestSample) {
	    			biggestSample = d;
	            }
	    	}
	    	// System.out.println("Biggest sample = " + biggestSample);
	    }
	    

	    
		public void normalize() {
	    	for (int i=0; i<staContainer.length; i++) {
	    		staContainer[i] = staContainer[i]/biggestSample;
	    	}
	    }
	    

	    public double getXScaleFactor(int panelWidth){
	    	double retval;
	    		retval =  (panelWidth / ((double) staContainer.length));
	    	return retval;
	    }

	    public double getYScaleFactor(int panelHeight){
	        return (panelHeight / (biggestSample * 2));
	    }

	    
	    public double[] getData(){
	        return staContainer;
	    }
	    

	    protected int getIncrement(double xScale) {
	        try {
	            int increment = (int) Math.floor((staContainer.length / (staContainer.length * xScale )));
	            if(increment==0) System.out.println("StaInfo WARNING: Increment = 0 ");
	            return increment;
	        } catch (Exception e) {
	            e.printStackTrace();
	        }
	        return -1;
	    }

	}




