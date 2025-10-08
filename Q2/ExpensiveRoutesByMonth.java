import java.io.IOException;
import org.apache.hadoop.conf.Configuration;
import org.apache.hadoop.fs.Path;
import org.apache.hadoop.io.DoubleWritable;
import org.apache.hadoop.io.LongWritable;
import org.apache.hadoop.io.Text;
import org.apache.hadoop.mapreduce.Job;
import org.apache.hadoop.mapreduce.Mapper;
import org.apache.hadoop.mapreduce.Partitioner;
import org.apache.hadoop.mapreduce.Reducer;
import org.apache.hadoop.mapreduce.lib.input.FileInputFormat;
import org.apache.hadoop.mapreduce.lib.output.FileOutputFormat;

public class ExpensiveRoutesByMonth {

    public static class RouteMapper extends Mapper<LongWritable, Text, Text, DoubleWritable> {
        private static final String TARGET_YEAR = "2013";

        public void map(LongWritable key, Text value, Context context) 
                throws IOException, InterruptedException {
            String line = value.toString();
            String[] fields = line.split(",");
            
            // Skip header
            if (fields[0].equalsIgnoreCase("key")) return;
            
            // Need at least 7 fields (key, fare, datetime, 4 coordinates)
            if (fields.length < 7) return;
            
            try {
                // Extract fare_amount (field[1])
                double fareAmount = Double.parseDouble(fields[1]);
                
                // Extract pickup_datetime (field[2])
                String pickupDatetime = fields[2];
                
                // Filter for TARGET_YEAR
                String year = pickupDatetime.substring(0, 4);
                if (!year.equals(TARGET_YEAR)) return;
                
                // Extract month
                String month = pickupDatetime.substring(5, 7);
                
                // Extract coordinates
                double plon = Double.parseDouble(fields[3]);
                double plat = Double.parseDouble(fields[4]);
                double dlon = Double.parseDouble(fields[5]);
                double dlat = Double.parseDouble(fields[6]);
                
                // Filter invalid coordinates
                if (plon == 0.0 || plat == 0.0 || dlon == 0.0 || dlat == 0.0) {
                    return;
                }
                
                // // NYC bounds check
                // if (plon < -75.0 || plon > -72.0 || plat < 39.0 || plat > 42.0 ||
                //     dlon < -75.0 || dlon > -72.0 || dlat < 39.0 || dlat > 42.0) {
                //     return;
                // }
                
                // Filter invalid fares (negative or extremely high)
                if (fareAmount <= 0.0 || fareAmount > 2000.0) {
                    return;
                }
                
                // Create route string with 4 decimal precision
                String route = String.format("%.5f,%.5f,%.5f,%.5f", plon, plat, dlon, dlat);
                
                // Composite key: month__route
                Text compositeKey = new Text(month + "__" + route);
                
                // Emit route with fare amount
                context.write(compositeKey, new DoubleWritable(fareAmount));
                
            } catch (Exception e) {
                // Skip malformed lines
            }
        }
    }

    public static class MonthPartitioner extends Partitioner<Text, DoubleWritable> {
        @Override
        public int getPartition(Text key, DoubleWritable value, int numPartitions) {
            String month = key.toString().split("__")[0];
            int monthNum = Integer.parseInt(month);
            return (monthNum - 1) % numPartitions;
        }
    }

    public static class MaxFareReducer extends Reducer<Text, DoubleWritable, Text, DoubleWritable> {
        private DoubleWritable result = new DoubleWritable();

        @Override
        public void reduce(Text key, Iterable<DoubleWritable> fares, Context context)
                throws IOException, InterruptedException {
            
            // Find maximum fare for this route
            double maxFare = Double.MIN_VALUE;
            for (DoubleWritable fare : fares) {
                if (fare.get() > maxFare) {
                    maxFare = fare.get();
                }
            }
            
            result.set(maxFare);
            context.write(key, result);
        }
    }

    public static void main(String[] args) throws Exception {
        if (args.length < 2) {
            System.err.println("Usage: ExpensiveRoutesByMonth <input> <output> [num_reducers]");
            System.exit(1);
        }
        
        Configuration conf = new Configuration();
        Job job = Job.getInstance(conf, "expensive routes by month 2013");
        job.setJarByClass(ExpensiveRoutesByMonth.class);
        job.setMapperClass(RouteMapper.class);
        job.setReducerClass(MaxFareReducer.class);
        job.setPartitionerClass(MonthPartitioner.class);
        
        int numReducers = (args.length >= 3) ? Integer.parseInt(args[2]) : 12;
        job.setNumReduceTasks(numReducers);
        
        job.setOutputKeyClass(Text.class);
        job.setOutputValueClass(DoubleWritable.class);
        
        FileInputFormat.addInputPath(job, new Path(args[0]));
        FileOutputFormat.setOutputPath(job, new Path(args[1]));
        
        System.exit(job.waitForCompletion(true) ? 0 : 1);
    }
}
