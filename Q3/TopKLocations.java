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

public class TopKLocations {
    
    // Mapper: Extract location type and location, emit type_location -> count
    public static class LocationMapper extends Mapper<LongWritable, Text, Text, IntWritable> {
        
        @Override
        public void map(LongWritable key, Text value, Context context) 
                throws IOException, InterruptedException {
            String line = value.toString().trim();
            if (line.isEmpty()) return;
            
            // Parse: "03__PICKUP__-73.9770,40.7450    1425"
            String[] parts = line.split("\\s+");
            if (parts.length < 2) return;
            
            try {
                String keyParts = parts[0];
                int count = Integer.parseInt(parts[1]);
                
                // Extract type and location (remove month prefix)
                String[] components = keyParts.split("__");
                if (components.length < 3) return;
                
                String type = components[1];      // "PICKUP" or "DROPOFF"
                String location = components[2];  // "-73.9770,40.7450"
                
                // Emit type__location -> count
                Text compositeKey = new Text(type + "__" + location);
                context.write(compositeKey, new IntWritable(count));
                
            } catch (Exception e) {
                // Skip
            }
        }
    }
    
    // Reducer: Aggregate counts across all months for each location
    public static class SumReducer extends Reducer<Text, IntWritable, Text, IntWritable> {
        private IntWritable result = new IntWritable();
        
        @Override
        public void reduce(Text key, Iterable<IntWritable> counts, Context context)
                throws IOException, InterruptedException {
            int sum = 0;
            for (IntWritable count : counts) {
                sum += count.get();
            }
            result.set(sum);
            context.write(key, result);
        }
    }
    
    // Mapper 2: Swap for sorting, keeping type separate
    public static class SortMapper extends Mapper<LongWritable, Text, Text, IntWritable> {
        
        @Override
        public void map(LongWritable key, Text value, Context context)
                throws IOException, InterruptedException {
            String line = value.toString().trim();
            if (line.isEmpty()) return;
            
            String[] parts = line.split("\\s+");
            if (parts.length < 2) return;
            
            try {
                String keyParts = parts[0];
                int totalCount = Integer.parseInt(parts[1]);
                
                String[] components = keyParts.split("__");
                if (components.length < 2) return;
                
                String type = components[0];      // "PICKUP" or "DROPOFF"
                String location = components[1];  // "-73.9770,40.7450"
                
                // Emit with count as part of key for sorting
                // Format: "TYPE|-count|location" (negative for descending sort)
                Text sortKey = new Text(type + "|" + String.format("%010d", 1000000000 - totalCount) + "|" + location);
                context.write(sortKey, new IntWritable(totalCount));
                
            } catch (Exception e) {
                // Skip
            }
        }
    }
    
    // Reducer 2: Select top K for each type
    public static class TopKReducer extends Reducer<Text, IntWritable, Text, IntWritable> {
        private int K = 5;
        private int pickupCount = 0;
        private int dropoffCount = 0;
        
        @Override
        protected void setup(Context context) {
            K = context.getConfiguration().getInt("topk.k", 5);
        }
        
        @Override
        public void reduce(Text key, Iterable<IntWritable> counts, Context context)
                throws IOException, InterruptedException {
            
            String keyStr = key.toString();
            String[] parts = keyStr.split("\\|");
            if (parts.length < 3) return;
            
            String type = parts[0];
            String location = parts[2];
            
            // Get the actual count
            int count = 0;
            for (IntWritable c : counts) {
                count = c.get();
                break;
            }
            
            // Check if we should emit based on type and current count
            if (type.equals("PICKUP")) {
                if (pickupCount >= K) return;
                context.write(new Text("PICKUP: " + location), new IntWritable(count));
                pickupCount++;
            } else if (type.equals("DROPOFF")) {
                if (dropoffCount >= K) return;
                context.write(new Text("DROPOFF: " + location), new IntWritable(count));
                dropoffCount++;
            }
        }
    }
    
    public static void main(String[] args) throws Exception {
        if (args.length < 2) {
            System.err.println("Usage: TopKLocations <input_path> <output_path> [k]");
            System.exit(1);
        }
        
        Configuration conf = new Configuration();
        int k = (args.length >= 3) ? Integer.parseInt(args[2]) : 5;
        conf.setInt("topk.k", k);
        
        // Job 1: Aggregate counts across all months
        Job job1 = Job.getInstance(conf, "aggregate location counts");
        job1.setJarByClass(TopKLocations.class);
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
        
        // Job 2: Sort and select top K for each type
        Job job2 = Job.getInstance(conf, "top k locations");
        job2.setJarByClass(TopKLocations.class);
        job2.setMapperClass(SortMapper.class);
        job2.setReducerClass(TopKReducer.class);
        job2.setNumReduceTasks(1);
        
        job2.setOutputKeyClass(Text.class);
        job2.setOutputValueClass(IntWritable.class);
        
        FileInputFormat.addInputPath(job2, tempPath);
        FileOutputFormat.setOutputPath(job2, new Path(args[1]));
        
        System.exit(job2.waitForCompletion(true) ? 0 : 1);
    }
}
