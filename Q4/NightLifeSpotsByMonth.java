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

public class NightlifeSpotsByMonth {

    public static class NightlifeMapper extends Mapper<LongWritable, Text, Text, IntWritable> {
        private final static IntWritable one = new IntWritable(1);
        private static final String TARGET_YEAR = "2013";

        public void map(LongWritable key, Text value, Context context) 
                throws IOException, InterruptedException {
            String line = value.toString();
            String[] fields = line.split(",");
            
            // Skip header
            if (fields[0].equalsIgnoreCase("key")) return;
            
            if (fields.length < 7) return;
            
            try {
                // Extract pickup_datetime (field[2])
                String pickupDatetime = fields[2];
                
                // Filter for TARGET_YEAR
                String year = pickupDatetime.substring(0, 4);
                if (!year.equals(TARGET_YEAR)) return;
                
                // Extract month
                String month = pickupDatetime.substring(5, 7);
                
                // Extract hour from datetime (format: "YYYY-MM-DD HH:MM:SS UTC")
                // Hour is at position 11-12
                String hourStr = pickupDatetime.substring(11, 13);
                int hour = Integer.parseInt(hourStr);
                
                // Filter for nightlife hours: 20:00-23:59 (20-23) OR 00:00-02:59 (0-2)
                if (!((hour >= 20 && hour <= 23) || (hour >= 0 && hour <= 2))) {
                    return;
                }
                
                // Extract dropoff coordinates
                double dlon = Double.parseDouble(fields[5]);
                double dlat = Double.parseDouble(fields[6]);
                
                // Filter invalid coordinates
                if (dlon == 0.0 || dlat == 0.0) {
                    return;
                }
                
                // NYC bounds check
                if (dlon < -75.0 || dlon > -72.0 || dlat < 39.0 || dlat > 42.0) {
                    return;
                }
                
                // Format dropoff location with 4 decimal precision
                String dropoffLoc = String.format("%.4f,%.4f", dlon, dlat);
                
                // Emit with month prefix
                Text compositeKey = new Text(month + "__" + dropoffLoc);
                context.write(compositeKey, one);
                
            } catch (Exception e) {
                // Skip malformed lines
            }
        }
    }

    public static class MonthPartitioner extends Partitioner<Text, IntWritable> {
        @Override
        public int getPartition(Text key, IntWritable value, int numPartitions) {
            // Extract month from key (format: "MM__location")
            String month = key.toString().split("__")[0];
            int monthNum = Integer.parseInt(month);
            return (monthNum - 1) % numPartitions;
        }
    }

    public static class LocationReducer extends Reducer<Text, IntWritable, Text, IntWritable> {
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
            System.err.println("Usage: NightlifeSpotsByMonth <input> <output> [num_reducers]");
            System.exit(1);
        }
        
        Configuration conf = new Configuration();
        Job job = Job.getInstance(conf, "nightlife spots by month 2013");
        job.setJarByClass(NightlifeSpotsByMonth.class);
        job.setMapperClass(NightlifeMapper.class);
        job.setReducerClass(LocationReducer.class);
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
