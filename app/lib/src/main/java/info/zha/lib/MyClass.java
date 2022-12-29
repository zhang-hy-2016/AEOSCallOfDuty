package info.zha.lib;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.InputStream;
import java.io.Reader;
import java.io.FileReader;
import java.io.IOException;
import org.apache.commons.csv.CSVFormat;
import org.apache.commons.csv.CSVParser;
import org.apache.commons.csv.CSVRecord;

import java.util.Arrays;
import java.util.Calendar;
import java.util.GregorianCalendar;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Properties;
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
        //File aeos_plan = new File("E:\\099_Temp\\aeos_example.csv");
        File aeos_plan = new File("E:\\099_Temp\\aeos_dutyplan.csv");

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
                        String week = "kw" + i;
                        if (v.contains("b")) {
                            // add user into duty plan
                            dutyPlan.put(week,user);
                        }
                    }
                }
            }

            // output
            checkDutyPlan(dutyPlan);

        } catch (IOException e){
            e.printStackTrace();
        }

    }

    public static void checkDutyPlan(Map<String,String> dutyPlan){
        SortedSet<String> keys = new TreeSet<String>(dutyPlan.keySet());

        for (String week: keys) {
            System.out.println(week + ":" +  dutyPlan.get(week));
        }

        String dutyPerson = getDutyPerson(dutyPlan);
        System.out.println("Today duty:" +  dutyPerson);
    }

    /**
     * get man on duty from current week .
     * @param dutyPlan
     * @return An empty string for not matched search.
     */
    public static String getDutyPerson(Map<String,String> dutyPlan){
        // Setup Calender, kw1 = the first full week
        Calendar now = GregorianCalendar.getInstance(Locale.GERMANY);
        now.setFirstDayOfWeek(Calendar.MONDAY);
        now.setMinimalDaysInFirstWeek(4); // 4 is ISO 8601 standard compatible setting

        String weekNum = "kw"+now.get(Calendar.WEEK_OF_YEAR);
        return dutyPlan.containsKey(weekNum)?dutyPlan.get(weekNum): "";
    }

    public static void calFirstWeek(){
        Calendar now = GregorianCalendar.getInstance(Locale.GERMANY);
        now.setFirstDayOfWeek(Calendar.MONDAY);
        now.setMinimalDaysInFirstWeek(4); // 4 is ISO 8601 standard compatible setting


        System.out.println(now.getWeeksInWeekYear());       // don't use this method
        System.out.println(now.get(Calendar.WEEK_OF_YEAR));  // Correct week number

    }

    public static void propertiesTest() {
        try {

            File appPropertiesFile = new File("E:\\012_WorkSpace\\android_ws\\AEOSRufnummer\\app\\src\\main\\assets\\app.properties");
            InputStream input = new FileInputStream(appPropertiesFile);
            Properties appProperties = new Properties();
            appProperties.load(input);

            String call_forward_auto=appProperties.getProperty("call.forwarding.auto.vodafone");
            System.out.println(call_forward_auto);
            System.out.println(call_forward_auto.replaceAll("Zielrufnummer","111111111"));

            System.out.println(appProperties.getProperty("phone.u5" ));




        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public static void main(String[] args) {
        //test1();
        //test2();
        propertiesTest();
    }
}