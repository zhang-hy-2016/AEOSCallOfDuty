package info.zha.lib;

import java.io.BufferedReader;
import java.io.File;
import java.io.Reader;
import java.io.FileReader;
import java.io.IOException;
import org.apache.commons.csv.CSVFormat;
import org.apache.commons.csv.CSVParser;
import org.apache.commons.csv.CSVRecord;

import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;
import java.util.SortedSet;
import java.util.TreeSet;

public class MyClass {

    public static void test1(){
        File aeos_plan = new File("E:\\099_Temp\\aeos_example.csv");

        BufferedReader reader;
        try {
            reader = new BufferedReader(new FileReader(aeos_plan));
            String line = reader.readLine();

            while (line != null) {
                System.out.println(line);
                String[] attributes = line.split(";");

                System.out.println(attributes.length);
                System.out.println(Arrays.toString(attributes));

                // read next line
                line = reader.readLine();


            }

            reader.close();
        } catch (IOException e){
            e.printStackTrace();
        }


    }

    public static void test2() {
        File aeos_plan = new File("E:\\099_Temp\\aeos_example.csv");

        /*
          dutyPlan example:
          kw_1 - user1
          kw_2 - user3
          ...
          kw_52 - userx
         */
        Map<String, String> dutyPlan = new HashMap<>();
        try {
            Reader in = new FileReader(aeos_plan);

            CSVFormat csvFormat =  CSVFormat.Builder.create(CSVFormat.EXCEL)
                                    .setDelimiter(';')
                                    .build();

            Iterable<CSVRecord> records = csvFormat.parse(in);
            boolean head_line = true;
            for (CSVRecord record : records) {
                int column_size = 0;
                if (head_line){
                    for (int i = 1; i < record.size(); i++){
                        // create week index
                        dutyPlan.put(record.get(i), "");
                    }
                    head_line = false;
                } else {
                    String user = record.get(0);
                    for (int i = 1; i < record.size(); i++ ) {
                        String v = record.get(i);
                        String week = String.format("kw%o", i);
                        if (v.contains("b")) {
                            // add user into duty plan
                            dutyPlan.put(week,user);
                        }
                    }
                }
            }

            // output
            SortedSet<String> keys = new TreeSet<String>(dutyPlan.keySet());

            for (String week: keys) {
                System.out.println(week + ":" +  dutyPlan.get(week));
            }

        } catch (IOException e){
            e.printStackTrace();
        }

    }
    public static void main(String[] args) {
        //test1();
        test2();
    }
}