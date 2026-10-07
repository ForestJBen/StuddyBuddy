package studdybuddy;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockHttpSession;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest(properties = "spring.datasource.url=jdbc:h2:mem:admin_demo_test")
@AutoConfigureMockMvc
@Import(AdminDemoConfiguration.class)
class AdminDemoIntegrationTests {
    @Autowired MockMvc mvc;
    @Test void demoLoginOpensDashboardAndProfile() throws Exception {
        var result = mvc.perform(post("/api/auth/login").with(csrf())
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"email\":\"admin@gmail.com\",\"password\":\"admin123456789\"}"))
                .andExpect(status().isOk()).andExpect(jsonPath("$.redirectUrl").value("/admin"))
                .andExpect(jsonPath("$.user.role").value("ADMIN")).andReturn();
        var session = (MockHttpSession) result.getRequest().getSession(false);
        mvc.perform(get("/admin").session(session)).andExpect(status().isOk())
                .andExpect(content().string(org.hamcrest.Matchers.containsString("admin@gmail.com")));
        mvc.perform(get("/profile").session(session)).andExpect(status().isOk())
                .andExpect(content().string(org.hamcrest.Matchers.containsString("Admin dashboard")));
    }
    @Test @WithMockUser(username="admin@gmail.com", roles = "STUDENT") void studentCannotOpenAdmin() throws Exception {
        mvc.perform(get("/admin")).andExpect(status().isForbidden());
        mvc.perform(get("/api/admin/users")).andExpect(status().isForbidden());
    }
    @Test void anonymousCannotOpenAdmin() throws Exception {
        mvc.perform(get("/admin")).andExpect(status().is3xxRedirection());
        mvc.perform(get("/login?admin=true")).andExpect(status().isOk())
                .andExpect(content().string(org.hamcrest.Matchers.containsString("Log in to admin dashboard")));
    }
}
