package didclab.cse.buffalo.hysterisis;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;
import au.com.bytecode.opencsv.CSVReader;


public class Similarity {
	static double []avgAttributes,variance;
	private static double similarityThreshold = 0.999;	
	public static void  readFile(List<Entry> entries, String fname) {
		try{
		 	CSVReader reader = new CSVReader(new FileReader(fname), ',');
		 	System.out.println("Reading "+fname+"...");
	        //read line by line
		 	String []header = reader.readNext();
	        Map<String, Integer> attributeIndices = new HashMap<String, Integer>();
	        for(int i = 0; i< header.length; i++)
	        	attributeIndices.put(header[i], i);
	        int id = 0;
	        String[] record = null;
	        while((record = reader.readNext()) != null){
	        	Entry entry = new Entry(); 
	        	try
	        	{
	        		//Mandatory attributes
		        	entry.setId(id++);
		        	entry.setFileSize( Double.parseDouble(record[attributeIndices.get("FileSize")]) );
		        	
		        	entry.setFileCount( Integer.parseInt(record[attributeIndices.get("FileCount")]) );
		        	entry.setSource( record[attributeIndices.get("Source")] );
		        	entry.setDestination( record[attributeIndices.get("Destination")] );
		        	entry.setBandwidth( Double.parseDouble( record[attributeIndices.get("Bandwidth")] ) );
		        	if(entry.getBandwidth() >= Math.pow(10, 10))
		        		entry.setBandwidth( entry.getBandwidth()*1024*1024*1024.0 );
		        	entry.setRtt( Double.parseDouble( record[attributeIndices.get("RTT")]) );
		        	entry.setBufferSize( Double.parseDouble( record[attributeIndices.get("BufferSize")]) );
		        	entry.setParallellism( Integer.parseInt( record[attributeIndices.get("Parallelism")]) );
		        	entry.setConcurrency( Integer.parseInt( record[attributeIndices.get("Concurrency")]) );
		        	entry.setPipelining( Integer.parseInt( record[attributeIndices.get("Pipelining")] ) );
		        	if(record[attributeIndices.get("Fast")].compareTo("ON") == 0 || 
		        			record[attributeIndices.get("Fast")].compareTo("1") == 0)
			        		entry.setFast(true);
		        	
		        	//Optional attributes
		        	if(attributeIndices.containsKey(("TestBed") ))
		        		entry.setTestbed( record[attributeIndices.get("TestBed")] );
		        	entry.setThroughput( Double.parseDouble( record[attributeIndices.get("Throughput")]) );
		        	if(attributeIndices.containsKey("Emulation"))
		        		if(record[attributeIndices.get("Emulation")].compareTo("REAL") != 0)
		        			entry.setEmulation(true);
		        	if(attributeIndices.containsKey("Dedicated"))
		        		if(record[attributeIndices.get("Dedicated")].compareTo("true") != 0)
		        			entry.setEmulation(true);
		        	if(attributeIndices.containsKey(("Note") ))
		        		entry.setNote( record[attributeIndices.get("Note")] );
		        	entry.setDensity( Entry.findDensityOfList(entry.getFileSize(), (entry.getBandwidth()*entry.getRtt()/8.0)) );
	        	}
	        	catch (Exception e){
	        		e.printStackTrace();
	        		System.exit(0);
	        	}
	            entry.calculateSpecVector();
	            entries.add(entry);
	        }
	        reader.close();
		}
		catch (Exception e){
			e.printStackTrace();
			System.exit(0);
		}
   }
	
	
	static public void normalizeDataset(List<Entry> entries){
        double[] sumSpecVector = null;
		for (Entry entry:entries) {
			 if(sumSpecVector == null)
	            	sumSpecVector = new double[entry.specVector.size()];
	            for (int i = 0; i < sumSpecVector.length; i++) {
					sumSpecVector[i]+=entry.specVector.get(i);
				}
		}
		
		Similarity.avgAttributes = new double[sumSpecVector.length];
        for (int i = 0; i < Similarity.avgAttributes.length; i++) {
        	Similarity.avgAttributes[i] =0.0;
		}
        System.out.println("Average attributes");
        for (int i = 0; i < sumSpecVector.length; i++) {
        	Similarity.avgAttributes[i] = sumSpecVector[i]/entries.size();
        	round(Similarity.avgAttributes[i],3);
			System.out.print(Similarity.avgAttributes[i]+"*");
		}
        System.out.println();
        Similarity.variance = new double[sumSpecVector.length];
        for (int i = 0; i < Similarity.variance.length; i++) {
        	Similarity.variance[i] =0.0;
		}
        for (Entry e: entries) {
        	for (int i = 0; i < e.specVector.size(); i++) {
        		Similarity.variance[i] += Math.pow(e.specVector.get(i)-Similarity.avgAttributes[i],2);
			}
		}
        System.out.println("Variance attributes");
        for (int i = 0; i < Similarity.variance.length; i++) {
        	Similarity.variance[i] = Math.sqrt(Similarity.variance[i]/entries.size());
        	System.out.print(Similarity.variance[i]+"*");
		}
        System.out.println();
        for (Entry e: entries) {
        	for (int i = 0; i < e.specVector.size(); i++) {
        		double newValue = (e.specVector.get(i)-Similarity.avgAttributes[i]);
        		if(Similarity.variance[i] != 0 )
        			newValue/= Similarity.variance[i];
        		e.specVector.set(i, newValue) ;
        		
			}
		}
	}
	public static double round(double value, int places) {
	    if (places < 0) throw new IllegalArgumentException();

	    BigDecimal bd = new BigDecimal(value);
	    bd = bd.setScale(places, RoundingMode.HALF_UP);
	    return bd.doubleValue();
	}
	public Map<Entry,Double> measureCosineSimilarity(Entry target, List<Entry> entries){
		
		Map<Entry, Double> cosineSimilarity =new HashMap<Entry,Double>();	
		
		// List of spec vector elements
		//1-bandwidth
		//2-rtt
		//3-bandwidth*rtt/(8.0*bufferSize)
		//4-DensityToValue(density)*1.0
		//5
			//if (isDedicated) 	specVector.add(0.0)
			//else 				specVector.add(1.0)
		//6-fileSize/(1024*1024))---DISABLED 
		//6-specVector.add(fileCount)
		//7- Testbed name
		
		double[] weights = {4,3,8,10,10,4,10};
		
		/*
		double sumWeight = 0;
		for (int i = 0; i < weights.length; i++) {
			sumWeight +=weights[i];
		}
		
		System.out.println("Weights:");
		for (int i = 0; i < weights.length; i++) {
			weights[i] = weights[i]/sumWeight;
			System.out.println(weights[i]);
		}
		*/
		for (Entry e: entries) {
			
			double similarityValue = 0;
			
			
			target.specVector.add(1.0);
			if(target.getTestbed() != null && target.getTestbed().compareTo(e.getTestbed()) == 0)
				e.specVector.add(1.0);
			else 
				e.specVector.add(0.0);
			
			
			//Cosine Similarity
			double squareOne = 0, squareTwo= 0 , multiplication = 0;
			
			for (int i = 0; i < e.specVector.size(); i++) {
				double value1 = e.specVector.get(i) * weights[i];
				double value2 = target.specVector.get(i) *  weights[i];
				squareOne += (value1 * value1);
				squareTwo += (value2 * value2);
				multiplication += (value1 * value2);
				
			}
			similarityValue = multiplication/(Math.sqrt(squareOne)*Math.sqrt(squareTwo));
			e.specVector.remove(e.specVector.size()-1);
			target.specVector.remove(target.specVector.size()-1);
			
			//e.specVector.remove(e.specVector.size()-1);
			//End of cosine-similarity
			
			/*
			//Pearson-correlation 
			double squareOne = 0, squareTwo= 0 , multiplication = 0;
			double total1= 0 , total2 = 0;
			for (int i = 0; i < e.specVector.size(); i++) {
				total1 += e.specVector.get(i);
				total2 += target.specVector.get(i);
			}
			double mean1 = total1/e.specVector.size();
			double  mean2 = total2/e.specVector.size();
			for (int i = 0; i < e.specVector.size(); i++) {
				double value1 = e.specVector.get(i)-mean1;
				double value2 = target.specVector.get(i)-mean2;
				squareOne += (value1 * value1);
				squareTwo += (value2 * value2);
				multiplication += (value1 * value2);
				
			}
			double similarityValue = multiplication/(Math.sqrt(squareOne)*Math.sqrt(squareTwo));
			//Pearson-correlation 
			
			
			 if(e.throughput == 108.929651069 || e.throughput == 8531.8550352391 || e.throughput ==1918.02612453 
					|| e.throughput ==3340.78852179){
				.printEntry(e, "");
				System.out.println("mult:"+multiplication+"\tsquareOne:"+squareOne+"\tsquareTwo:"+squareTwo+"\tcosine:"+similarityValue);
			}
			/*if(e.testbed.compareTo("XSEDE") == 0 && e.fileSize> Math.pow(10, 9)){
				System.out.println(e.testbed + "\t"+ e.source+"\t"+e.destination+"\tDensity:"+e.density
		        		+"\tFileSize"+e.fileSize+"\tFileCount:"+e.fileCount+"\tSimilarity"+similarityValue);
			}*/
			e.similarityValue = similarityValue;
			cosineSimilarity.put(e, similarityValue);
		}
		//System.out.println("similarity size:"+cosineSimilarity.size());
		ValueComparator bvc =  new ValueComparator(cosineSimilarity);
        TreeMap<Entry,Double> sorted_map = new TreeMap<Entry,Double>(bvc);
        sorted_map.putAll(cosineSimilarity);
        //System.out.println("sorted size:"+sorted_map.size());
//		return sorted_map;
        return cosineSimilarity;
	}
	
	
	
	/*
	 * This function takes list of entries and a target entry 
	 * Returns list of entries which is similar to target entry based on cosine similarity values 
	 */
	static List<Entry> findSimilarEntries(List<Entry> entries, Entry targetEntry){
		//Normalize values of target entry
		for (int j = 0; j < targetEntry.specVector.size(); j++) {
			double newValue = (targetEntry.specVector.get(j)-Similarity.avgAttributes[j]);
			if(Similarity.variance[j] != 0 )
				newValue /=Similarity.variance[j];
			targetEntry.specVector.set(j, newValue);
		}
		
		
		Map<Entry,Double> similarEntries = new TreeMap<Entry,Double>();
		Similarity similarity = new Similarity();
		similarEntries = similarity.measureCosineSimilarity(targetEntry,entries);
		
		
		//Entry.printEntry(targetEntry,"");
		Map<Double, Entry> mostSimilarEntries = null;
		List<Entry> mostSimilarEntries_ = null;
		List<Double> mostSimilarEntriesThroughput = null;
		
		
		Integer counter = 0;
		//Decrease similarity threshold value until having at least 30 entry
		while (counter < 30){
			counter = 0;
			mostSimilarEntries = new HashMap<Double,Entry>();
			mostSimilarEntries_ = new LinkedList<Entry>();
			mostSimilarEntriesThroughput = new LinkedList<Double>();
			Iterator<Map.Entry<Entry,Double>> it = similarEntries.entrySet().iterator();
	    	while (it.hasNext()) {
				Map.Entry<Entry,Double> pairs = (Map.Entry<Entry,Double>)it.next();
		        Entry e = pairs.getKey();
		        double value = pairs.getValue();
				if(value > similarityThreshold){
					mostSimilarEntries.put(e.getThroughput(),e);
					mostSimilarEntriesThroughput.add(e.getThroughput());
					mostSimilarEntries_.add(e);
					counter++;
				}
	    	}
	    	if(counter < 30){
	    		if(similarityThreshold > 0.999)
		    		similarityThreshold -= 0.001;
	    		else if(similarityThreshold > 0.9)
		    		similarityThreshold -= 0.01;
		    	else
		    		similarityThreshold -= 0.1;
		    	System.out.println("Similarity threshold updated:"+similarityThreshold);
	    	}
		}
		//removeMultipleOccurences(mostSimilarEntries, mostSimilarEntriesThroughput);
    	System.out.println("most similar list Size:"+mostSimilarEntries.size());
    	
		
		return mostSimilarEntries_;
	}
	
	class ValueComparator implements Comparator<Entry> {

	    Map<Entry, Double> base;
	    public ValueComparator(Map<Entry, Double> base) {
	        this.base = base;
	    }

	    // Note: this comparator imposes orderings that are inconsistent with equals.    
	    public int compare(Entry a, Entry b) {
	    	if(Math.abs((base.get(a) - base.get(b))) < 0.0000001){
	    		if(a.getThroughput() > b.getThroughput())
	    			return -1;
	    		else 
	    			return 1;
	    	}
	    	else if (base.get(a) > base.get(b)) {
	            return -1;
	        }
	        
	        else {
	            return 1;
	        } // returning 0 would merge keys
	    }
	}
	
	//sort entries based on date of the transfer 
	public static int[] categorizeEntries(int chunkNumber, LinkedList<LinkedList<Entry>> trials, List<Entry> similarEntries){
		//trials = new LinkedList<Map<String,Similarity.Entry>>();
    	Map<String,Entry> set = new HashMap<String,Entry>();
    	LinkedList<Entry> list = new LinkedList<Entry>();
    	
        Collections.sort(similarEntries, new DateComparator());
    	for  (Entry e: similarEntries) {
			if(set.containsKey(e.getIdentity())){
				//Entry s = set.get(e.getIdentity());
				//System.out.println("Existing entry:"+s.getIdentity()+" "+s.throughput+" "+s.date.toString());
				//System.out.println("New entry"+e.getIdentity()+" "+e.throughput+" "+e.date.toString());
				//Map<String,Similarity.Entry> copied = new HashMap<String,Similarity.Entry>(set);
				//trials.add((LinkedList)list.clone());
				if(list.size() >= 6*6*6)
					trials.add(list);
				list =  new LinkedList<Entry>();
				set.clear();
			}
			list.add(e);
			set.put(e.getIdentity(), e);
    	}
        
		trials.add(list);
		System.out.println("Sets:"+trials.size());
		
		
		parameterBorderCheck(trials);
		int i=0;
		int maxCC, maxP, maxPPQ;
		maxCC = maxP = maxPPQ = Integer.MIN_VALUE;
		for (LinkedList<Entry> subset : trials){
			try{
				File f = new File("outputs/chunk_"+chunkNumber);
				if(!f.exists())
					f.mkdir();
				FileWriter writer = new FileWriter("outputs/chunk_"+chunkNumber+"/trial-"+(i++)+".txt");
				//Iterator<Map.Entry<String,Entry>> iterator = subset.entrySet().iterator();
				for (Entry entry : subset){
					/* Max parameters observed in the logs */
			        maxPPQ = Math.max(maxPPQ, entry.getPipelining());
			        maxP = Math.max(maxP, entry.getParallellism());
			        maxCC = Math.max(maxCC, entry.getConcurrency());
			        int fast = entry.getFast() == true ? 1 : 0;
					writer.write(entry.getConcurrency()+" "+entry.getParallellism()+" "+entry.getPipelining()+" "+
							 fast+" "+entry.getThroughput()+"\n");
				}
				writer.flush();
				writer.close();
			}
			catch(Exception e){
				e.printStackTrace();
			}
		}
		return (new int[]{maxCC, maxP, maxPPQ});
	}
	
	private static void parameterBorderCheck(LinkedList<LinkedList<Entry>> trials){
		for (LinkedList<Entry> chunk : trials){
			for (Entry entry :  chunk){
				entry.setPipelining( entry.getPipelining() == -1 ? 0 : entry.getPipelining() );
				entry.setParallellism( entry.getParallellism() == -1 ? 1 : entry.getParallellism() ) ;
				entry.setConcurrency(entry.getConcurrency() == -1 ?  1 :entry.getConcurrency());
				
				entry.setConcurrency((int) (Math.min(entry.getConcurrency(), entry.getFileCount() ) ) );
				entry.setPipelining( (int)Math.min( entry.getPipelining(),(Math.max(entry.getFileCount() - entry.getConcurrency(), 0))));
			}
		}
	}
	
	static class DateComparator implements Comparator<Entry> {
		
	    // Note: this comparator imposes orderings that are inconsistent with equals.    
	    @Override
	    public int compare(Entry a, Entry b) {
	    	if(a.getDate().before(b.getDate()))
	    		return -1;
	        else 
	            return 1;
	    }
	}
	
	
	
}
