package kr.jmlab.spring.ai.agent.book.chapter4.support;

import java.util.Comparator;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;

import org.springframework.ai.tool.annotation.Tool;
import org.springframework.ai.tool.annotation.ToolParam;
import org.springframework.stereotype.Component;

/**
 * 4.5 최종 프로젝트에서 사용하는 간단한 메모리 기반 할 일 툴.
 */
@Component
public class TodoTools {

    private final AtomicInteger sequence = new AtomicInteger(1);
    private final Map<Integer, Task> tasks = new ConcurrentHashMap<>();

    @Tool(
            name = ToolNames.TODO_ADD,
            description = "사용자의 할 일을 하나 추가하고 새 할 일 번호를 반환합니다.")
    public String addTodo(@ToolParam(description = "추가할 할 일 제목") String title) {
        int id = sequence.getAndIncrement();
        tasks.put(id, new Task(id, title, false));
        return "할 일 #%d 를 추가했습니다: %s".formatted(id, title);
    }

    @Tool(
            name = ToolNames.TODO_LIST,
            description = "현재 등록된 할 일 목록을 반환합니다.")
    public String listTodos() {
        if (tasks.isEmpty()) {
            return "등록된 할 일이 없습니다.";
        }

        return tasks.values().stream()
                .sorted(Comparator.comparingInt(Task::id))
                .map(task -> "%d. [%s] %s".formatted(
                        task.id(),
                        task.completed() ? "완료" : "진행",
                        task.title()))
                .reduce((left, right) -> left + "\n" + right)
                .orElse("등록된 할 일이 없습니다.");
    }

    @Tool(
            name = ToolNames.TODO_COMPLETE,
            description = "할 일 번호를 받아 해당 할 일을 완료 처리합니다.")
    public String completeTodo(@ToolParam(description = "완료할 할 일 번호") int id) {
        Task task = tasks.get(id);
        if (task == null) {
            return "할 일 #%d 를 찾을 수 없습니다.".formatted(id);
        }
        tasks.put(id, new Task(task.id(), task.title(), true));
        return "할 일 #%d 를 완료 처리했습니다.".formatted(id);
    }

    private record Task(int id, String title, boolean completed) {
    }
}
