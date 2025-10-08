import java.io.IOException;
import org.apache.hadoop.conf.Configuration;
import org.apache.hadoop.fs.Path;
import org.apache.hadoop.io.IntWritable;
import org.apache.hadoop.io.LongWritable;
import org.apache.hadoop.io.Text;
import org.apache.hadoop.io.WritableComparable;
import org.apache.hadoop.io.WritableComparator;
import org.apache.hadoop.mapreduce.Job;
import org.apache.hadoop.mapreduce.Mapper;
import org.apache.hadoop.mapreduce.Reducer;
import org.apache.hadoop.mapreduce.lib.input.FileInputFormat;
import org.apache.hadoop.mapreduce.lib.output.FileOutputFormat;

public class TopKNightlifeSpots {
    
    // Mapper: Extract location (without month) and emit location -> count
    public static class LocationMapper extends Mapper<LongWritable, Text, Text, IntWritable> {
        
        @Override
        public void map(LongWritable key, Text value, Context context) 
                throws IOException, InterruptedException {
            String line = value.toString().trim();
            if (line.isEmpty()) return;
            
            // Parse: "03__-73.9770,40.7450    1425"
            String[] parts = line.split("\\s+");
            if (parts.length < 2) return;
            
            try {
                String keyPart = parts[0];
                int count = Integer.parseInt(parts[1]);
                
                // Extract location (remove month prefix)
                String[] components = keyPart.split("__");
                if (components.length < 2) return;
                String location = components[1];
                
                // Emit location -> count
                context.write(new Text(location), new IntWritable(count));
                
            } catch (Exception e) {
                // Skip
            }
        }
    }
    
    // Reducer: Aggregate counts across all months for each location
    public static class SumReducer extends Reducer<Text, IntWritable, Text, IntWritable> {
        private IntWritable result = new IntWritable();
        
        @Override
        public void reduce(Text location, Iterable<IntWritable> counts, Context context)
                throws IOException, InterruptedException {
            int sum = 0;
            for (IntWritable count : counts) {
                sum += count.get();
            }
            result.set(sum);
            context.write(location, result);
        }
    }
    
    // Mapper 2: Swap location and count for sorting
    public static class SortMapper extends Mapper<LongWritable, Text, IntWritable, Text> {
        
        @Override
        public void map(LongWritable key, Text value, Context context)
                throws IOException, InterruptedException {
            String line = value.toString().trim();
            if (line.isEmpty()) return;
            
            String[] parts = line.split("\\s+");
            if (parts.length < 2) return;
            
            try {
                String location = parts[0];
                int totalCount = Integer.parseInt(parts[1]);
                
                // Emit count -> location (for sorting by count)
                context.write(new IntWritable(totalCount), new Text(location));
            } catch (Exception e) {
                // Skip
            }
        }
    }
    
    // Comparator: Sort counts in DESCENDING order
    public static class DescendingIntComparator extends WritableComparator {
        protected DescendingIntComparator() {
            super(IntWritable.class, true);
        }
        
        @Override
        public int compare(WritableComparable a, WritableComparable b) {
            return -1 * a.compareTo(b);
        }
    }
    
    // Reducer 2: Select top K spots
    public static class TopKReducer extends Reducer<IntWritable, Text, Text, IntWritable> {
        private int K = 5;
        private int count = 0;
        
        @Override
        protected void setup(Context context) {
            K = context.getConfiguration().getInt("topk.k", 5);
        }
        
        @Override
        public void reduce(IntWritable totalCount, Iterable<Text> locations, Context context)
                throws IOException, InterruptedException {
            
            for (Text location : locations) {
                if (this.count >= K) return;
                context.write(location, totalCount);
                this.count++;
            }
        }
    }
    
    public static void main(String[] args) throws Exception {
        if (args.length < 2) {
            System.err.println("Usage: TopKNightlifeSpots <input_path> <output_path> [k]");
            System.exit(1);
        }
        
        Configuration conf = new Configuration();
        int k = (args.length >= 3) ? Integer.parseInt(args[2]) : 5;
        conf.setInt("topk.k", k);
        
        // Job 1: Aggregate counts across all months
        Job job1 = Job.getInstance(conf, "aggregate nightlife spot counts");
        job1.setJarByClass(TopKNightlifeSpots.class);
        job1.setMapperClass(LocationMapper.class);
        job1.setCombinerClass(SumReducer.class);
        job1.setReducerClass(SumReducer.class);
        job1.setOutputKeyClass(Text.class);
        job1.setOutputValueClass(IntWritable.class);
        
        Path tempPath = new Path(args[1] + "_temp");
        FileInputFormat.addInputPath(job1, new Path(args[0]));
        FileOutputFormat.setOutputPath(job1, tempPath);
        
        if (!job1.waitForCompletion(true)) {
            System.exit(1);
        }
        
        // Job 2: Sort by count and select top K
        Job job2 = Job.getInstance(conf, "top k nightlife spots");
        job2.setJarByClass(TopKNightlifeSpots.class);
        job2.setMapperClass(SortMapper.class);
        job2.setReducerClass(TopKReducer.class);
        job2.setSortComparatorClass(DescendingIntComparator.class);
        job2.setNumReduceTasks(1);
        
        job2.setMapOutputKeyClass(IntWritable.class);
        job2.setMapOutputValueClass(Text.class);
        job2.setOutputKeyClass(Text.class);
        job2.setOutputValueClass(IntWritable.class);
        
        FileInputFormat.addInputPath(job2, tempPath);
        FileOutputFormat.setOutputPath(job2, new Path(args[1]));
        
        System.exit(job2.waitForCompletion(true) ? 0 : 1);
    }
}
