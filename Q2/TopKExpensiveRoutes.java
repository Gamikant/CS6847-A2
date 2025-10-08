import java.io.IOException;
import org.apache.hadoop.conf.Configuration;
import org.apache.hadoop.fs.Path;
import org.apache.hadoop.io.DoubleWritable;
import org.apache.hadoop.io.LongWritable;
import org.apache.hadoop.io.Text;
import org.apache.hadoop.io.WritableComparable;
import org.apache.hadoop.io.WritableComparator;
import org.apache.hadoop.mapreduce.Job;
import org.apache.hadoop.mapreduce.Mapper;
import org.apache.hadoop.mapreduce.Reducer;
import org.apache.hadoop.mapreduce.lib.input.FileInputFormat;
import org.apache.hadoop.mapreduce.lib.output.FileOutputFormat;

public class TopKExpensiveRoutes {
    
    // Mapper: Extract route (without month) and emit route -> max_fare
    public static class ExpensiveMapper extends Mapper<LongWritable, Text, Text, DoubleWritable> {
        
        @Override
        public void map(LongWritable key, Text value, Context context) 
                throws IOException, InterruptedException {
            String line = value.toString().trim();
            if (line.isEmpty()) return;
            
            // Parse: "03__-73.977,40.745,-73.981,40.743    42.5"
            String[] parts = line.split("\\s+");
            if (parts.length < 2) return;
            
            try {
                String routeWithMonth = parts[0];
                double fare = Double.parseDouble(parts[1]);
                
                // Extract route (remove month prefix)
                String[] routeParts = routeWithMonth.split("__");
                if (routeParts.length < 2) return;
                String route = routeParts[1];
                
                // Emit route -> fare
                context.write(new Text(route), new DoubleWritable(fare));
                
            } catch (Exception e) {
                // Skip
            }
        }
    }
    
    // Reducer: Find maximum fare across all months for each route
    public static class MaxReducer extends Reducer<Text, DoubleWritable, Text, DoubleWritable> {
        private DoubleWritable result = new DoubleWritable();
        
        @Override
        public void reduce(Text route, Iterable<DoubleWritable> fares, Context context)
                throws IOException, InterruptedException {
            double maxFare = Double.MIN_VALUE;
            for (DoubleWritable fare : fares) {
                if (fare.get() > maxFare) {
                    maxFare = fare.get();
                }
            }
            result.set(maxFare);
            context.write(route, result);
        }
    }
    
    // Mapper 2: Swap fare and route for sorting
    public static class SortMapper extends Mapper<LongWritable, Text, DoubleWritable, Text> {
        
        @Override
        public void map(LongWritable key, Text value, Context context)
                throws IOException, InterruptedException {
            String line = value.toString().trim();
            if (line.isEmpty()) return;
            
            String[] parts = line.split("\\s+");
            if (parts.length < 2) return;
            
            try {
                String route = parts[0];
                double maxFare = Double.parseDouble(parts[1]);
                
                // Emit fare -> route (for sorting by fare)
                context.write(new DoubleWritable(maxFare), new Text(route));
            } catch (Exception e) {
                // Skip
            }
        }
    }
    
    // Comparator: Sort fares in DESCENDING order
    public static class DescendingDoubleComparator extends WritableComparator {
        protected DescendingDoubleComparator() {
            super(DoubleWritable.class, true);
        }
        
        @Override
        public int compare(WritableComparable a, WritableComparable b) {
            return -1 * a.compareTo(b);
        }
    }
    
    // Reducer 2: Select top K routes
    public static class TopKReducer extends Reducer<DoubleWritable, Text, Text, DoubleWritable> {
        private int K = 5;
        private int count = 0;
        
        @Override
        protected void setup(Context context) {
            K = context.getConfiguration().getInt("topk.k", 5);
        }
        
        @Override
        public void reduce(DoubleWritable fare, Iterable<Text> routes, Context context)
                throws IOException, InterruptedException {
            
            for (Text route : routes) {
                if (this.count >= K) return;
                context.write(route, fare);
                this.count++;
            }
        }
    }
    
    public static void main(String[] args) throws Exception {
        if (args.length < 2) {
            System.err.println("Usage: TopKExpensiveRoutes <input_path> <output_path> [k]");
            System.exit(1);
        }
        
        Configuration conf = new Configuration();
        int k = (args.length >= 3) ? Integer.parseInt(args[2]) : 5;
        conf.setInt("topk.k", k);
        
        // Job 1: Aggregate max fares across all months
        Job job1 = Job.getInstance(conf, "aggregate max fares");
        job1.setJarByClass(TopKExpensiveRoutes.class);
        job1.setMapperClass(ExpensiveMapper.class);
        job1.setCombinerClass(MaxReducer.class);
        job1.setReducerClass(MaxReducer.class);
        job1.setOutputKeyClass(Text.class);
        job1.setOutputValueClass(DoubleWritable.class);
        
        Path tempPath = new Path(args[1] + "_temp");
        FileInputFormat.addInputPath(job1, new Path(args[0]));
        FileOutputFormat.setOutputPath(job1, tempPath);
        
        if (!job1.waitForCompletion(true)) {
            System.exit(1);
        }
        
        // Job 2: Sort by fare and select top K
        Job job2 = Job.getInstance(conf, "top k expensive routes");
        job2.setJarByClass(TopKExpensiveRoutes.class);
        job2.setMapperClass(SortMapper.class);
        job2.setReducerClass(TopKReducer.class);
        job2.setSortComparatorClass(DescendingDoubleComparator.class);
        job2.setNumReduceTasks(1);
        
        job2.setMapOutputKeyClass(DoubleWritable.class);
        job2.setMapOutputValueClass(Text.class);
        job2.setOutputKeyClass(Text.class);
        job2.setOutputValueClass(DoubleWritable.class);
        
        FileInputFormat.addInputPath(job2, tempPath);
        FileOutputFormat.setOutputPath(job2, new Path(args[1]));
        
        System.exit(job2.waitForCompletion(true) ? 0 : 1);
    }
}
