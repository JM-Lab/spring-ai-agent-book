package kr.jmlab.spring.ai.agent.book.chapter6.capability.local;

import java.time.LocalDate;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.time.zone.ZoneRulesException;

import org.springframework.ai.tool.annotation.Tool;
import org.springframework.ai.tool.annotation.ToolParam;

import kr.jmlab.spring.ai.agent.book.chapter6.capability.ToolNames;

/**
 * {@link Tool @Tool} 애너테이션으로 등록하는 날짜/시간 툴.
 */
public class DateTimeTools {

    private static final String DEFAULT_ZONE_ID = "Asia/Seoul";
    private static final DateTimeFormatter DATE_TIME_FORMATTER =
            DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss z");

    @Tool(
            name = ToolNames.CURRENT_DATETIME,
            description = "현재 날짜와 시간을 IANA 시간대 기준으로 반환합니다. 시간대가 없으면 Asia/Seoul을 사용합니다.")
    public String currentDateTime(
            @ToolParam(required = false, description = "IANA 시간대 ID. 예: Asia/Seoul, America/New_York")
            String zoneId) {
        try {
            String normalizedZoneId = normalizeZoneId(zoneId);
            return ZonedDateTime.now(java.time.ZoneId.of(normalizedZoneId))
                    .format(DATE_TIME_FORMATTER);
        } catch (ZoneRulesException ex) {
            return "알 수 없는 시간대입니다. 예: Asia/Seoul, UTC, America/New_York";
        }
    }

    @Tool(
            name = ToolNames.CURRENT_DATE,
            description = "오늘 날짜를 yyyy-MM-dd 형식으로 반환합니다.")
    public String currentDate() {
        return LocalDate.now().toString();
    }

    private String normalizeZoneId(String zoneId) {
        if (zoneId == null || zoneId.isBlank()) {
            return DEFAULT_ZONE_ID;
        }
        return zoneId.trim();
    }
}
