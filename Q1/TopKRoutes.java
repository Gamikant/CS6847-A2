import java.io.IOException;
import java.util.*;
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

public class TopKRoutes {
    
    // Mapper: Extract route (without month) and emit route -> count
    public static class TopKMapper extends Mapper<LongWritable, Text, Text, IntWritable> {
        
        @Override
        public void map(LongWritable key, Text value, Context context) 
                throws IOException, InterruptedException {
            String line = value.toString().trim();
            
            // Skip empty lines
            if (line.isEmpty()) return;
            
            // Parse: "03__-73.977,40.745,-73.981,40.743    1425"
            String[] parts = line.split("\\s+");
            if (parts.length < 2) return;
            
            try {
                String routeWithMonth = parts[0];  // "03__-73.977,40.745,-73.981,40.743"
                int count = Integer.parseInt(parts[1]);
                
                // Extract just the route (remove month prefix)
                String[] routeParts = routeWithMonth.split("__");
                if (routeParts.length < 2) return;
                String route = routeParts[1];  // "-73.977,40.745,-73.981,40.743"
                
                // Emit route -> count (will be aggregated by reducer)
                context.write(new Text(route), new IntWritable(count));
                
            } catch (Exception e) {
                // Skip malformed lines
            }
        }
    }
    
    // Combiner/Reducer: Sum counts for each route across all months
    public static class SumReducer extends Reducer<Text, IntWritable, Text, IntWritable> {
        private IntWritable result = new IntWritable();
        
        @Override
        public void reduce(Text route, Iterable<IntWritable> counts, Context context)
                throws IOException, InterruptedException {
            int sum = 0;
            for (IntWritable count : counts) {
                sum += count.get();
            }
            result.set(sum);
            context.write(route, result);
        }
    }
    
    // Mapper 2: Swap route and total count for sorting
    public static class SortMapper extends Mapper<LongWritable, Text, IntWritable, Text> {
        
        @Override
        public void map(LongWritable key, Text value, Context context)
                throws IOException, InterruptedException {
            String line = value.toString().trim();
            if (line.isEmpty()) return;
            
            String[] parts = line.split("\\s+");
            if (parts.length < 2) return;
            
            try {
                String route = parts[0];
                int totalCount = Integer.parseInt(parts[1]);
                
                // Emit count -> route (for sorting by count)
                context.write(new IntWritable(totalCount), new Text(route));
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
    
    // Reducer 2: Select top K routes
    public static class TopKReducer extends Reducer<IntWritable, Text, Text, IntWritable> {
        private int K = 5;
        private int count = 0;
        
        @Override
        protected void setup(Context context) {
            K = context.getConfiguration().getInt("topk.k", 5);
        }
        
        @Override
        public void reduce(IntWritable totalCount, Iterable<Text> routes, Context context)
                throws IOException, InterruptedException {
            
            for (Text route : routes) {
                if (this.count >= K) return;
                context.write(route, totalCount);
                this.count++;
            }
        }
    }
    
    public static void main(String[] args) throws Exception {
        if (args.length < 2) {
            System.err.println("Usage: TopKRoutes <input_path> <output_path> [k]");
            System.exit(1);
        }
        
        Configuration conf = new Configuration();
        int k = (args.length >= 3) ? Integer.parseInt(args[2]) : 5;
        conf.setInt("topk.k", k);
        
        // Job 1: Aggregate counts across all months
        Job job1 = Job.getInstance(conf, "aggregate route counts");
        job1.setJarByClass(TopKRoutes.class);
        job1.setMapperClass(TopKMapper.class);
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
        Job job2 = Job.getInstance(conf, "top k routes");
        job2.setJarByClass(TopKRoutes.class);
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
