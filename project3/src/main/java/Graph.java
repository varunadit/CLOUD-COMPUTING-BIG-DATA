import java.io.*;
import java.util.*;

import org.apache.hadoop.conf.Configuration;
import org.apache.hadoop.fs.Path;
import org.apache.hadoop.mapreduce.*;
import org.apache.hadoop.io.*;
import org.apache.hadoop.mapreduce.lib.input.*;
import org.apache.hadoop.mapreduce.lib.output.*;


class Vertex implements Writable {
    public short tag;
    public long group;
    public long VID;
    public long[] adjacent;
	public int size;

	Vertex(){
	}

	Vertex(short tag, long group, long VID, long[] adjacent){
		this.tag=tag;
		this.group=group;
		this.VID=VID;
		this.size = adjacent.length;
		this.adjacent = new long[size];
		this.adjacent = adjacent;
	}

	Vertex(short tag,long group)
	{
		this.tag=tag;
		this.group=group;

	}

	public void write(DataOutput out) throws IOException {
		out.writeShort(tag);
		out.writeLong(group);
	    out.writeLong(VID);
		out.writeInt(size);
	    for (int i=0;i<size;i++)
	    {
	        out.writeLong(adjacent[i]);
	    }	        
	}

	public void readFields (DataInput in ) throws IOException {
		tag= in.readShort();
		group = in.readLong();
		VID = in.readLong();
		size = in.readInt();
		adjacent=new long[size];
		
		for (int y=0;y<size;y++)
		{
			adjacent[y]=in.readLong();
		}
	}
}



public class Graph {

	public static class FirstGraphMapper extends Mapper<Object,Text,LongWritable,Vertex > {
        @Override
        public void map ( Object key, Text value, Context context )
                        throws IOException, InterruptedException {
            Scanner sc = new Scanner(value.toString()).useDelimiter(",");
            Vector<Long> adj_vector = new Vector<Long>();
            long vid = sc.nextLong();
            while(sc.hasNextLong()) {
				long variable = sc.nextLong();
				adj_vector.addElement(variable);
			}
			long[] adjacent = new long[adj_vector.size()];
			for(int i=0;i<adj_vector.size();i++){
				adjacent[i]=adj_vector.get(i);
			}
            context.write(new LongWritable(vid),new Vertex((short)0,vid,vid, adjacent));
            sc.close();
        }
    }

	public static class FirstGraphReducer extends Reducer<LongWritable,Vertex,LongWritable,Vertex> {
		@Override
		public void reduce ( LongWritable key, Iterable<Vertex> values, Context context )
						   throws IOException, InterruptedException {
			for (Vertex v:values) {
				context.write(key,new Vertex(v.tag,v.group,v.VID,v.adjacent));						
			}
		}	
	}

	public static class SecondGraphMapper extends Mapper<LongWritable,Vertex,LongWritable,Vertex > {	
	    @Override       
	    public void map ( LongWritable key, Vertex vertex, Context context )
	                     throws IOException, InterruptedException {
	        context.write(new LongWritable(vertex.VID),vertex);
	         
	        for (long n:vertex.adjacent) {
	 			context.write(new LongWritable(n),new Vertex((short)1,vertex.group));
	 		}
	    }
	}

	public static class SecondGraphReducer extends Reducer<LongWritable,Vertex,LongWritable,Vertex> {
		@Override
		public void reduce ( LongWritable key, Iterable<Vertex> vertices, Context context )
						   throws IOException, InterruptedException {
			long m=Long.MAX_VALUE;
			Vertex vertex_clone=new Vertex();
			for (Vertex v:vertices) {
				if(v.tag == 0) {
					vertex_clone=new Vertex(v.tag,v.group,v.VID,v.adjacent);
				}
				m=Long.min(m,v.group);
			}
			context.write(new LongWritable(m),new Vertex((short)0,m,vertex_clone.VID,vertex_clone.adjacent));				             
		}
	}

	public static class ThirdGraphMapper extends Mapper<LongWritable,Vertex,LongWritable,LongWritable> {
	        @Override
	    public void map ( LongWritable key, Vertex value, Context context )
	                    throws IOException, InterruptedException {
			context.write(key,new LongWritable(1));
	    }
	}		

	public static class ThirdGraphReducer extends Reducer<LongWritable,LongWritable,LongWritable,LongWritable> {        
	        @Override
	    public void reduce ( LongWritable key, Iterable<LongWritable> values, Context context )
	                       throws IOException, InterruptedException {
	        	long m=0L;
	    	for(LongWritable v:values) {
    			m = m + v.get();
	    	}
	    	context.write(key,new LongWritable(m));
	    }
	}


    public static void main ( String[] args ) throws Exception {

		Job job1 = Job.getInstance();
    	job1.setJobName("Job1");
    	job1.setJarByClass(Graph.class);

    	job1.setMapperClass(FirstGraphMapper.class);
		job1.setReducerClass(FirstGraphReducer.class);

    	job1.setOutputKeyClass(LongWritable.class);
    	job1.setOutputValueClass(Vertex.class);

    	job1.setMapOutputKeyClass(LongWritable.class);
		job1.setMapOutputValueClass(Vertex.class);
	
		job1.setInputFormatClass(TextInputFormat.class);
    	job1.setOutputFormatClass(SequenceFileOutputFormat.class);
        

    	FileInputFormat.setInputPaths(job1, new Path(args[0]));
		FileOutputFormat.setOutputPath(job1, new Path(args[1] + "/f0"));
	
	
		job1.waitForCompletion(true);

		
        for ( short i = 0; i < 5; i++ ) {
			Job job2= Job.getInstance();
			job2.setJobName("MapReduceJob2 : Pass " + i);
			job2.setJarByClass(Graph.class);

			job2.setMapperClass(SecondGraphMapper.class);
        	job2.setReducerClass(SecondGraphReducer.class);

	    	job2.setOutputKeyClass(LongWritable.class);
			job2.setOutputValueClass(Vertex.class);

			job2.setMapOutputKeyClass(LongWritable.class);
			job2.setMapOutputValueClass(Vertex.class);

			job2.setInputFormatClass(SequenceFileInputFormat.class);
			job2.setOutputFormatClass(SequenceFileOutputFormat.class);
			 
 	    	FileInputFormat.setInputPaths(job2, new Path(args[1] + "/f" + i));
 	       	FileOutputFormat.setOutputPath(job2, new Path(args[1]+ "/f" + (i+1)));

 	       	job2.waitForCompletion(true);
		}
        
		Job job3= Job.getInstance();
		job3.setJobName("FinalJob");
		job3.setJarByClass(Graph.class);

    	job3.setMapperClass(ThirdGraphMapper.class);
    	job3.setReducerClass(ThirdGraphReducer.class);

		job3.setOutputKeyClass(LongWritable.class);
		job3.setOutputValueClass(LongWritable.class);
	
	    job3.setMapOutputKeyClass(LongWritable.class);
	    job3.setMapOutputValueClass(LongWritable.class);

	    job3.setInputFormatClass(SequenceFileInputFormat.class);
		job3.setOutputFormatClass(TextOutputFormat.class);

	    FileInputFormat.setInputPaths(job3, new Path(args[1] + "/f5"));
		FileOutputFormat.setOutputPath(job3, new Path(args[2]));

	    job3.waitForCompletion(true);
		
    }
}