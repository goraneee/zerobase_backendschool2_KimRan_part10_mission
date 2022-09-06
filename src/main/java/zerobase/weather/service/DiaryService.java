package zerobase.weather.service;
import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;
import org.json.simple.parser.ParseException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Isolation;
import org.springframework.transaction.annotation.Transactional;
import zerobase.weather.WeatherApplication;
import zerobase.weather.domain.DateWeather;
import zerobase.weather.domain.Diary;
import zerobase.weather.error.InvalidDate;
import zerobase.weather.repository.DiaryRepository;
import zerobase.weather.repository.DateWeatherRepository;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.time.LocalDate;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
@Transactional(readOnly = true)
public class DiaryService {

    @Value("${openweathermap.key}")
    private String apiKey;

    private final DiaryRepository diaryRepository;
    private final DateWeatherRepository dateWeatherRepository;

    private static final Logger logger = LoggerFactory.getLogger(WeatherApplication.class);

    public DiaryService(DiaryRepository diaryRepository, DateWeatherRepository dateWeatherRepository) {
        this.diaryRepository = diaryRepository;
        this.dateWeatherRepository = dateWeatherRepository;
    }

    @Transactional
    @Scheduled(cron = "0 0 1 * * *")
    public void saveWeatherDate(){
        dateWeatherRepository.save(getWeatherFromApi());
    }
    private DateWeather getDateWeather(LocalDate date){

        List<DateWeather> dateWeatherListFromDB = dateWeatherRepository.findAllByDate(date);

        // DB에 정보가 없는 경우
        if(dateWeatherListFromDB.size() == 0){
            return  getWeatherFromApi();
        }
        return dateWeatherListFromDB.get(0);    //왜 0이지?? 1개밖에 없나?

    }

    @Transactional(isolation = Isolation.SERIALIZABLE)
    public void createDiary(LocalDate date, String text){

        logger.info("== Started to create diary ==");
        // 날씨 데이터 가져오기
        DateWeather dateWeather = getDateWeather(date);

        // 파싱된 데이터와 일기 값을 내 데이터베이스에 넣기
        Diary nowDiary = new Diary();
        nowDiary.setDateWeather(dateWeather);
        nowDiary.setText(text);
        diaryRepository.save(nowDiary); // DB에 데이터 넣는다.
        logger.info("== End to create diary ==");
    }

    private DateWeather getWeatherFromApi(){
        // openweathermap에서  날씨 데이터 가져오기
        String weatherData = getWeatherString();
        // 받아온 날씨 json 파싱
        Map<String, Object> parsedWeather = parseWeather(weatherData);

        // 파싱된 날짜 데이터를 엔티티에 넣어준다.
        DateWeather dateWeather = new DateWeather();
        dateWeather.setDate(LocalDate.now());  // 날씨를 가져오는 시점의 데이터를 넣어준다.
        dateWeather.setWeather(parsedWeather.get("main").toString());
        dateWeather.setIcon(parsedWeather.get("icon").toString());
        dateWeather.setTemperature((Double) parsedWeather.get("temp"));

        return  dateWeather;
    }

    // 1 Read
    @Transactional(readOnly = true)
    public List<Diary> readDiary(LocalDate date) {

//        if(date.isAfter(LocalDate.ofYearDay(3050, 1))){
//            throw new InvalidDate();
//        }
        return  diaryRepository.findAllByDate(date);
    }

    public List<Diary> readDiaries(LocalDate startDate, LocalDate endDate) {
        return  diaryRepository.findAllByDateBetween(startDate, endDate);
    }

    // 2 Update
    public void updateDiary(LocalDate date, String text) {
        Diary nowDiary = diaryRepository.getFirstByDate(date);
        nowDiary.setText(text);
        diaryRepository.save(nowDiary);
    }

    // 3 Delete
    public void deleteDiary(LocalDate date){
        diaryRepository.deleteAllByDate(date);
    }

    private String getWeatherString(){
        String apiUrl = "https://api.openweathermap.org/data/2.5/weather?q=seoul&appid=" + apiKey;

        try {
            URL url = new URL(apiUrl);
            HttpURLConnection connection = (HttpURLConnection) url.openConnection();  //  apiUrl을 HttpURL 형식으로 연결
            connection.setRequestMethod("GET"); // get 요청
            int responseCode = connection.getResponseCode();   // 요청을 보낸 다음에 받아온 응답 결과를 응답 코드(상태 코드)로 받아올 수 있다.

            BufferedReader br;
            if(responseCode == 200){    // 정상
                br = new BufferedReader(new InputStreamReader(connection.getInputStream()));
            }else{
                br = new BufferedReader(new InputStreamReader(connection.getErrorStream()));
            }

            String inputLine;
            StringBuilder response = new StringBuilder();   // 결괏값을 response에 쌓는다.

            while((inputLine = br.readLine()) != null){
                response.append(inputLine);
            }
            br.close();
            return response.toString();

        } catch (Exception e) {
            return "failed to get response!";
        }
    }

    // temp(온도), main, icon을 Map형태로 반환해준다.
    private Map<String, Object> parseWeather(String jsonString){

        JSONParser jsonParser = new JSONParser();
        JSONObject jsonObject;  // parsing 결괏값

        try{
            jsonObject = (JSONObject) jsonParser.parse(jsonString);     // 파싱 결과
        }catch (ParseException e){
            throw new RuntimeException(e);
        }

        Map<String, Object> resultMap = new HashMap<>();

        JSONObject mainData = (JSONObject) jsonObject.get("main");          // main의 value
        resultMap.put("temp", mainData.get("temp"));
        JSONArray weatherArray = (JSONArray) jsonObject.get("weather");   //
        JSONObject weatherData = (JSONObject) weatherArray.get(0);   //
        resultMap.put("main", weatherData.get("main"));
        resultMap.put("icon", weatherData.get("icon"));
        return resultMap;
    }

}
