package noticeboard;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Comparator;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.TimeZone;

import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RestController;

import com.alibaba.fastjson.JSON;

@RestController
public class NoticeController {

    @RequestMapping(value = "/notices", method = RequestMethod.POST)
    public List<Map<String, Object>> getMoles(@RequestBody String params) {
        System.out.println(new Date() + "/" + params);
        List<Map<String, Object>> list = new ArrayList<Map<String, Object>>();

        File data = new File("data");
        File[] files = data.listFiles();
        List<File> fileList = new ArrayList<File>();
        for (File f : files) {
            String filename = f.getName();
            if (filename.startsWith("donate") && filename.endsWith(".csv")) {
                fileList.add(f);
            }
        }

        fileList.sort(new Comparator<File>() {
            @Override
            public int compare(File o1, File o2) {
                int d1 = Integer.parseInt(o1.getName().substring(6, 14));
                int d2 = Integer.parseInt(o2.getName().substring(6, 14));
                return d2 - d1;
            }
        });

        try (FileReader fr = new FileReader(fileList.get(0)); //
                BufferedReader in = new BufferedReader(fr);) {
            in.readLine();
            String str;
            while ((str = in.readLine()) != null) {
                String[] ss = str.split(",");
                String time = ss[2];

                Map<String, Object> o = new HashMap<String, Object>();
                o.put("ts", getTs(time));

                String method = ss[8];
                o.put("method", method);

                String commodity = ss[10];
                o.put("commodity", commodity);

                String brand = ss[13];
                o.put("brand", brand);

                String type = ss[14];
                o.put("type", type);

                double amount = 0;
                if ("A.捐款".equals(method)) {
                    amount = str2double(ss[9]);
                } else if ("B.捐物".equals(method)) {
                    amount = str2double(ss[15]);
                } else {
                    System.out.println(str);
                }
                o.put("amount", amount);

                String name = ss[7];
                if ("A.可以".equals(ss[6])) {
                    name = ss[5];
                }
                o.put("name", name);

                String remark = "";
                if (ss.length > 19) {
                    remark = ss[19];
                }
                o.put("remark", remark);

                list.add(o);
            }
        } catch (IOException e) {
        }

        List<Map<String, Object>> result = new ArrayList<Map<String, Object>>();
        Map<String, Object> paramMap = JSON.parseObject(params);
        Double theAmount = null;
        try {
            theAmount = Double.parseDouble(String.valueOf(paramMap.remove("amount")));
        } catch (NumberFormatException e1) {
        }

        polaris: for (Map<String, Object> o : list) {
            if (Objects.nonNull(theAmount)) {
                double amount = (double) o.get("amount");
                if (amount < (theAmount - 0.1) || amount > (theAmount + 0.1)) {
                    continue polaris;
                }
            }

            for (Map.Entry<String, Object> entry : paramMap.entrySet()) {
                String value = String.valueOf(entry.getValue());
                if (Objects.nonNull(value) && !value.isEmpty()) {
                    String key = entry.getKey();

                    if (o.containsKey(key)) {
                        String theValue = String.valueOf(o.get(key));
                        if (!value.equals(theValue)) {
                            continue polaris;
                        }
                    }
                }
            }
            result.add(o);
        }

        result.sort(new Comparator<Map<String, Object>>() {
            @Override
            public int compare(Map<String, Object> o1, Map<String, Object> o2) {
                long ts1 = (Long) o1.get("ts");
                long ts2 = (Long) o2.get("ts");
                return ts1 > ts2 ? -1 : (ts1 == ts2 ? 0 : 1);
            }
        });

        return result;
    }

    private double str2double(String str) {
        int start = 0;
        int end = str.length();
        if (str.charAt(start) == '"') {
            start++;
        }
        if (str.charAt(end - 1) == '"') {
            end--;
        }
        return Double.parseDouble(str.substring(start, end).trim());
    }

    private long getTs(String time) {
        String[] ss = time.split(" ");
        String[] ssdate = ss[0].split("/");
        String[] sstime = ss[1].split(":");
        int month = Integer.parseInt(ssdate[0]) - 1;
        int day = Integer.parseInt(ssdate[1]);
        int year = Integer.parseInt(ssdate[2]);

        int hour = Integer.parseInt(sstime[0]);
        int minute = Integer.parseInt(sstime[1]);

        Calendar calendar = Calendar.getInstance();
        calendar.setTimeZone(TimeZone.getTimeZone("PST"));
        calendar.set(year, month, day, hour, minute);

        return calendar.getTimeInMillis();
    }

}
