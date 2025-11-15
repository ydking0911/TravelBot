package com.yd.travelbot.domain.chatbot.domain.service;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.springframework.stereotype.Component;

@Component
public class MessageParser {

    public String extractCity(String input) {
        // 한국 주요 도시
        String[] koreanCities = {
            "서울", "부산", "제주", "제주도", "인천", "대구", "대전", "광주", "울산", 
            "수원", "성남", "고양", "용인", "청주", "천안", "전주", "포항", "창원"
        };
        
        // 주요 해외 도시 (한국어/영어)
        String[][] internationalCities = {
            {"도쿄", "Tokyo", "동경"},
            {"오사카", "Osaka", "대판"},
            {"교토", "Kyoto", "경도"},
            {"베이징", "Beijing", "북경", "Peking"},
            {"상하이", "Shanghai", "상해"},
            {"홍콩", "Hong Kong", "Hongkong"},
            {"타이베이", "Taipei", "대북"},
            {"방콕", "Bangkok", "방곡"},
            {"싱가포르", "Singapore", "신가포르"},
            {"쿠알라룸푸르", "Kuala Lumpur", "KL"},
            {"자카르타", "Jakarta", "자카르타"},
            {"마닐라", "Manila", "마닐라"},
            {"호치민", "Ho Chi Minh", "호치민시", "Saigon", "사이공"},
            {"하노이", "Hanoi", "하노이"},
            {"뉴욕", "New York", "NYC", "뉴욕시"},
            {"로스앤젤레스", "Los Angeles", "LA", "엘에이"},
            {"샌프란시스코", "San Francisco", "SF", "샌프란시스코"},
            {"시카고", "Chicago", "시카고"},
            {"라스베가스", "Las Vegas", "베가스", "LV"},
            {"마이애미", "Miami", "마이애미"},
            {"보스턴", "Boston", "보스턴"},
            {"시애틀", "Seattle", "시애틀"},
            {"런던", "London", "런던"},
            {"파리", "Paris", "파리"},
            {"로마", "Rome", "로마"},
            {"밀라노", "Milan", "밀라노"},
            {"베를린", "Berlin", "베를린"},
            {"뮌헨", "Munich", "뮌헨"},
            {"암스테르담", "Amsterdam", "암스테르담"},
            {"바르셀로나", "Barcelona", "바르셀로나"},
            {"마드리드", "Madrid", "마드리드"},
            {"리스본", "Lisbon", "리스본"},
            {"비엔나", "Vienna", "빈", "Wien"},
            {"프라하", "Prague", "프라하"},
            {"부다페스트", "Budapest", "부다페스트"},
            {"아테네", "Athens", "아테네"},
            {"이스탄불", "Istanbul", "이스탄불"},
            {"두바이", "Dubai", "두바이"},
            {"도하", "Doha", "도하"},
            {"리야드", "Riyadh", "리야드"},
            {"카이로", "Cairo", "카이로"},
            {"케이프타운", "Cape Town", "케이프타운"},
            {"요하네스버그", "Johannesburg", "요하네스버그"},
            {"시드니", "Sydney", "시드니"},
            {"멜버른", "Melbourne", "멜버른"},
            {"오클랜드", "Auckland", "오클랜드"},
            {"몬트리올", "Montreal", "몬트리올"},
            {"토론토", "Toronto", "토론토"},
            {"밴쿠버", "Vancouver", "밴쿠버"},
            {"멕시코시티", "Mexico City", "멕시코시티"},
            {"리우데자네이루", "Rio de Janeiro", "리우", "Rio"},
            {"상파울루", "São Paulo", "상파울루", "Sao Paulo"},
            {"부에노스아이레스", "Buenos Aires", "부에노스아이레스"},
            {"리마", "Lima", "리마"},
            {"보고타", "Bogotá", "보고타", "Bogota"},
            {"델리", "Delhi", "뉴델리", "New Delhi", "델리"},
            {"뭄바이", "Mumbai", "뭄바이", "Bombay"},
            {"방갈로르", "Bangalore", "방갈로르"},
            {"콜카타", "Kolkata", "콜카타", "Calcutta"},
            {"방콕", "Bangkok", "방곡"},
            {"쿠알라룸푸르", "Kuala Lumpur", "KL"}
        };
        
        // 한국 도시 먼저 체크
        for (String city : koreanCities) {
            if (input.contains(city)) {
                return city;
            }
        }
        
        // 해외 도시 체크 (각 배열의 첫 번째 요소를 반환)
        for (String[] cityVariants : internationalCities) {
            for (String variant : cityVariants) {
                if (input.contains(variant)) {
                    return cityVariants[0]; // 첫 번째 요소 반환 (표준 이름)
                }
            }
        }
        
        // 도시를 찾지 못한 경우, LLM이나 Tools가 처리하도록 null 반환
        return null;
    }

    public LocalDate extractDate(String input) {
        Pattern datePattern = Pattern.compile("(\\d{4})[-/](\\d{1,2})[-/](\\d{1,2})");
        Matcher matcher = datePattern.matcher(input);
        if (matcher.find()) {
            try {
                int year = Integer.parseInt(matcher.group(1));
                int month = Integer.parseInt(matcher.group(2));
                int day = Integer.parseInt(matcher.group(3));
                return LocalDate.of(year, month, day);
            } catch (Exception e) {
                return null;
            }
        }
        return null;
    }

    public Integer extractNumber(String input) {
        Pattern numberPattern = Pattern.compile("(\\d+)\\s*명|(\\d+)\\s*인|(\\d+)\\s*guest");
        Matcher matcher = numberPattern.matcher(input);
        if (matcher.find()) {
            for (int i = 1; i <= matcher.groupCount(); i++) {
                if (matcher.group(i) != null) {
                    return Integer.parseInt(matcher.group(i));
                }
            }
        }
        return null;
    }

    public String extractCuisine(String input) {
        String[] cuisines = {"한식", "중식", "일식", "양식", "분식", "치킨", "피자", "햄버거"};
        for (String cuisine : cuisines) {
            if (input.contains(cuisine)) {
                return cuisine;
            }
        }
        return null;
    }

    public String extractCategory(String input) {
        String[] categories = {"박물관", "미술관", "공원", "해변", "산", "사찰", "성", "궁"};
        for (String category : categories) {
            if (input.contains(category)) {
                return category;
            }
        }
        return null;
    }

    public BigDecimal extractAmount(String input) {
        // "100만원", "1000만원", "1억원" 등 처리
        Pattern millionPattern = Pattern.compile("(\\d+(?:\\.\\d+)?)\\s*만");
        Matcher millionMatcher = millionPattern.matcher(input);
        if (millionMatcher.find()) {
            try {
                BigDecimal amount = new BigDecimal(millionMatcher.group(1));
                return amount.multiply(new BigDecimal("10000")); // 만원 = 10,000원
            } catch (Exception e) {
                // 계속 진행
            }
        }
        
        // "1억원", "10억원" 등 처리
        Pattern hundredMillionPattern = Pattern.compile("(\\d+(?:\\.\\d+)?)\\s*억");
        Matcher hundredMillionMatcher = hundredMillionPattern.matcher(input);
        if (hundredMillionMatcher.find()) {
            try {
                BigDecimal amount = new BigDecimal(hundredMillionMatcher.group(1));
                return amount.multiply(new BigDecimal("100000000")); // 억원 = 100,000,000원
            } catch (Exception e) {
                // 계속 진행
            }
        }
        
        // 일반 숫자 패턴 (만원, 억원이 아닌 경우)
        Pattern amountPattern = Pattern.compile("(\\d+(?:,\\d{3})*(?:\\.\\d+)?)");
        Matcher matcher = amountPattern.matcher(input.replaceAll("[,]", ""));
        if (matcher.find()) {
            try {
                return new BigDecimal(matcher.group(1));
            } catch (Exception e) {
                return null;
            }
        }
        return null;
    }

    public String extractCurrency(String input, String type) {
        String upperInput = input.toUpperCase();
        
        // "from"인 경우: 입력에서 출발 통화 찾기
        if ("from".equals(type)) {
            // 한국어 통화명 우선 체크 (원, 달러 등)
            if (input.contains("원") || input.contains("won")) {
                return "KRW";
            }
            if (input.contains("달러") || input.contains("dollar")) {
                return "USD";
            }
            if (input.contains("유로") || input.contains("euro")) {
                return "EUR";
            }
            if (input.contains("엔") || input.contains("yen")) {
                return "JPY";
            }
            
            // 통화 코드 체크 (KRW가 먼저 나오는지 확인)
            String[] currencies = {"KRW", "USD", "EUR", "JPY", "CNY", "GBP"};
            for (String currency : currencies) {
                if (upperInput.contains(currency)) {
                    // "USD로 변환" 같은 패턴이면 USD는 to 통화
                    if (upperInput.contains(currency + "로") || upperInput.contains(currency + " TO")) {
                        continue; // 이건 to 통화
                    }
                    return currency;
                }
            }
        }
        
        // "to"인 경우: 목표 통화 찾기
        if ("to".equals(type)) {
            // "USD로", "달러로" 같은 패턴 찾기
            if (input.contains("달러로") || input.contains("USD로") || 
                input.contains("dollar") || upperInput.contains("USD TO") || upperInput.contains("TO USD")) {
                return "USD";
            }
            if (input.contains("원으로") || input.contains("KRW로") || 
                input.contains("won") || upperInput.contains("KRW TO") || upperInput.contains("TO KRW")) {
                return "KRW";
            }
            if (input.contains("유로로") || input.contains("EUR로") || 
                input.contains("euro") || upperInput.contains("EUR TO") || upperInput.contains("TO EUR")) {
                return "EUR";
            }
            if (input.contains("엔으로") || input.contains("JPY로") || 
                input.contains("yen") || upperInput.contains("JPY TO") || upperInput.contains("TO JPY")) {
                return "JPY";
            }
            
            // 통화 코드 체크 (뒤에 나오는 통화)
            String[] currencies = {"USD", "KRW", "EUR", "JPY", "CNY", "GBP"};
            for (String currency : currencies) {
                if (upperInput.contains(currency)) {
                    // "100만원을 USD로" 같은 패턴에서 USD 찾기
                    if (upperInput.contains(currency + "로") || upperInput.contains("TO " + currency)) {
                        return currency;
                    }
                }
            }
        }
        
        return null;
    }
}

