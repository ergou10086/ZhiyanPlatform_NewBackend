package hbnu.project.zhiyanbackend.tasks.model.dto;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.checkerframework.checker.units.qual.A;
import org.checkerframework.checker.units.qual.N;

import java.time.LocalDate;

/**
 * 任务基本信息内部类
 * 用于投影查询结果
 *
 * @author ErgouTree
 */
@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
public class TaskBasicInfo {

    private Long id;

    private String title;

    private Long creatorId;

    private LocalDate dueDate;
}
