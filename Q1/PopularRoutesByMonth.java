import java.io.IOException;
import org.apache.hadoop.conf.Configuration;
import org.apache.hadoop.fs.Path;
import org.apache.hadoop.io.IntWritable;
import org.apache.hadoop.io.LongWritable;
import org.apache.hadoop.io.Text;
import org.apache.hadoop.mapreduce.Job;
import org.apache.hadoop.mapreduce.Mapper;
import org.apache.hadoop.mapreduce.Partitioner;
import org.apache.hadoop.mapreduce.Reducer;
import org.apache.hadoop.mapreduce.lib.input.FileInputFormat;
import org.apache.hadoop.mapreduce.lib.output.FileOutputFormat;

public class PopularRoutesByMonth {

    public static class RouteMapper extends Mapper<LongWritable, Text, Text, IntWritable> {
        private final static IntWritable one = new IntWritable(1);
        private static final String TARGET_YEAR = "2013";

        public void map(LongWritable key, Text value, Context context) throws IOException, InterruptedException {
            String line = value.toString();
            String[] fields = line.split(",");
            
            // Skip header line by content
            if (fields[0].equalsIgnoreCase("key")) return;
            
            if (fields.length < 7) return;
            
            try {
                // Extract pickup_datetime (field[2])
                String pickupDatetime = fields[2];
                
                // FILTER: Only process TARGET_YEAR (2013)
                String year = pickupDatetime.substring(0, 4);
                if (!year.equals(TARGET_YEAR)) return;
                
                // Extract month (01-12)
                String month = pickupDatetime.substring(5, 7);
                
                // Parse coordinates
                double plon = Double.parseDouble(fields[3]);
                double plat = Double.parseDouble(fields[4]);
                double dlon = Double.parseDouble(fields[5]);
                double dlat = Double.parseDouble(fields[6]);
                
                // FILTER OUT INVALID COORDINATES
                // Skip if any coordinate is exactly 0 (invalid data)
                if (plon == 0.0 || plat == 0.0 || dlon == 0.0 || dlat == 0.0) {
                    return;
                }
                
                // NYC bounds check (optional but recommended)
                // if (plon < -75.0 || plon > -72.0 || plat < 39.0 || plat > 42.0 ||
                //     dlon < -75.0 || dlon > -72.0 || dlat < 39.0 || dlat > 42.0) {
                //     return;
                // }
                
                String rounded = String.format("%.5f,%.5f,%.5f,%.5f", plon, plat, dlon, dlat);
                Text compositeKey = new Text(month + "__" + rounded);
                context.write(compositeKey, one);
            } catch (Exception e) {
                // Ignore parse errors
            }
        }
    }

    public static class MonthPartitioner extends Partitioner<Text, IntWritable> {
        @Override
        public int getPartition(Text key, IntWritable value, int numPartitions) {
            String month = key.toString().split("__")[0];
            int monthNum = Integer.parseInt(month);
            return (monthNum - 1) % numPartitions;
        }
    }

    public static class RouteReducer extends Reducer<Text, IntWritable, Text, IntWritable> {
        private IntWritable result = new IntWritable();

        @Override
        public void reduce(Text key, Iterable<IntWritable> values, Context context)
                throws IOException, InterruptedException {
            int sum = 0;
            for (IntWritable val : values) {
                sum += val.get();
            }
            result.set(sum);
            context.write(key, result);
        }
    }

    public static void main(String[] args) throws Exception {
        if (args.length < 2) {
            System.err.println("Usage: PopularRoutesByMonth <input> <output> [num_reducers]");
            System.exit(1);
        }
        
        Configuration conf = new Configuration();
        Job job = Job.getInstance(conf, "popular routes by month 2013");
        job.setJarByClass(PopularRoutesByMonth.class);
        job.setMapperClass(RouteMapper.class);
        job.setReducerClass(RouteReducer.class);
        job.setPartitionerClass(MonthPartitioner.class);
        
        int numReducers = (args.length >= 3) ? Integer.parseInt(args[2]) : 12;
        job.setNumReduceTasks(numReducers);
        
        job.setOutputKeyClass(Text.class);
        job.setOutputValueClass(IntWritable.class);
        FileInputFormat.addInputPath(job, new Path(args[0]));
        FileOutputFormat.setOutputPath(job, new Path(args[1]));
        System.exit(job.waitForCompletion(true) ? 0 : 1);
    }
}
