package studdybuddy.controller;

import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import studdybuddy.course.CourseRepository;
import studdybuddy.user.UserRepository;

@Controller
public class AdminController {
    private final CourseRepository courses;
    private final UserRepository users;
    private final studdybuddy.group.StudyGroupRepository groups;
    private final studdybuddy.group.GroupService groupService;
    private final studdybuddy.admin.AdminAuditRepository audits;
    public AdminController(CourseRepository courses, UserRepository users,
            studdybuddy.group.StudyGroupRepository groups, studdybuddy.group.GroupService groupService,
            studdybuddy.admin.AdminAuditRepository audits) {
        this.courses = courses;
        this.users = users; this.groups=groups; this.groupService=groupService; this.audits=audits;
    }
    @GetMapping("/admin")
    public String dashboard(Model model) {
        model.addAttribute("courses", courses.findAll());
        model.addAttribute("users", users.findAll());
        var allGroups=groups.findAll();
        var members=new java.util.LinkedHashMap<Long, java.util.List<studdybuddy.group.dto.GroupMemberResponse>>();
        for(var group:allGroups) members.put(group.getId(),groupService.getMembers(group.getId()));
        model.addAttribute("groups",allGroups);
        model.addAttribute("members",members);
        model.addAttribute("audits",audits.findTop50ByOrderByIdDesc());
        return "admin";
    }
}
