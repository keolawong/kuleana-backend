package com.kahua.tracker.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.kahua.tracker.dto.RegistrationRequest;
import com.kahua.tracker.entity.User;
import com.kahua.tracker.entity.UserSecurityQuestion;
import com.kahua.tracker.repository.UserRepository;
import com.kahua.tracker.repository.UserSecurityQuestionRepository;
import com.kahua.tracker.security.JwtTokenProvider;
import com.kahua.tracker.security.LoginRequest;
import com.kahua.tracker.service.TokenBlacklistService;
import com.kahua.tracker.service.UserAuthCacheService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

import static org.hamcrest.Matchers.containsString;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@ExtendWith(MockitoExtension.class)
class AuthControllerTest {

    private MockMvc mockMvc;

    @Mock
    private AuthenticationManager authenticationManager;

    @Mock
    private JwtTokenProvider tokenProvider;

    @Mock
    private TokenBlacklistService tokenBlacklistService;

    @Mock
    private UserRepository userRepository;

    @Mock
    private PasswordEncoder passwordEncoder;

    @Mock
    private UserSecurityQuestionRepository userSecurityQuestionRepository;

    @Mock
    private UserAuthCacheService userAuthCacheService;

    @InjectMocks
    private AuthController authController;

    private ObjectMapper objectMapper;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.standaloneSetup(authController).build();
        objectMapper = new ObjectMapper();
    }

    @Test
    void authenticateUser_shouldReturnCookieWithSecureAndSameSiteNone() throws Exception {
        LoginRequest loginRequest = new LoginRequest();
        loginRequest.setUsername("testuser");
        loginRequest.setPassword("password123");

        Authentication authentication = new UsernamePasswordAuthenticationToken(
                "testuser",
                "password123",
                List.of(new SimpleGrantedAuthority("ROLE_USER"))
        );

        when(authenticationManager.authenticate(any(UsernamePasswordAuthenticationToken.class))).thenReturn(authentication);
        when(tokenProvider.generateToken(authentication)).thenReturn("mock-jwt-token");

        mockMvc.perform(post("/api/auth/login")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(loginRequest)))
                .andExpect(status().isOk())
                .andExpect(header().string(HttpHeaders.SET_COOKIE, containsString("Secure")))
                .andExpect(header().string(HttpHeaders.SET_COOKIE, containsString("SameSite=None")))
                .andExpect(header().string(HttpHeaders.SET_COOKIE, containsString("HttpOnly")))
                .andExpect(jsonPath("$.username").value("testuser"))
                .andExpect(jsonPath("$.token").value("mock-jwt-token"));
    }

    @Test
    void logoutUser_shouldReturnCookieWithSecureAndSameSiteNone() throws Exception {
        mockMvc.perform(post("/api/auth/logout"))
                .andExpect(status().isOk())
                .andExpect(header().string(HttpHeaders.SET_COOKIE, containsString("Secure")))
                .andExpect(header().string(HttpHeaders.SET_COOKIE, containsString("SameSite=None")))
                .andExpect(header().string(HttpHeaders.SET_COOKIE, containsString("Max-Age=0")));
    }

    @Test
    void getChallengeQuestions_shouldReturnTwoQuestions() throws Exception {
        com.kahua.tracker.entity.User user = new com.kahua.tracker.entity.User();
        user.setId(java.util.UUID.randomUUID());
        user.setUsername("testuser");
        
        when(userRepository.findByUsername("testuser")).thenReturn(java.util.Optional.of(user));
        
        com.kahua.tracker.entity.UserSecurityQuestion q1 = new com.kahua.tracker.entity.UserSecurityQuestion();
        q1.setQuestion("Q1");
        com.kahua.tracker.entity.UserSecurityQuestion q2 = new com.kahua.tracker.entity.UserSecurityQuestion();
        q2.setQuestion("Q2");
        com.kahua.tracker.entity.UserSecurityQuestion q3 = new com.kahua.tracker.entity.UserSecurityQuestion();
        q3.setQuestion("Q3");
        
        when(userSecurityQuestionRepository.findByUserId(user.getId())).thenReturn(List.of(q1, q2, q3));
        
        mockMvc.perform(org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get("/api/auth/security-questions/challenge/testuser"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(2));
    }

    @Test
    void getSecurityQuestionPool_shouldReturnAllTwentyQuestions() throws Exception {
        mockMvc.perform(org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get("/api/auth/security-questions/pool"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(20))
                .andExpect(jsonPath("$[0]").value("What was the name of your first pet?"))
                .andExpect(jsonPath("$[19]").value("What was the name of your first stuffed animal or toy?"));
    }

    @Test
    void updateProfile_shouldNotEvictUserCacheAndPreserveAuthentication() throws Exception {
        User user = new User();
        user.setId(UUID.randomUUID());
        user.setUsername("testuser");
        user.setFirstName("OldFirst");
        user.setLastName("OldLast");

        when(userRepository.findByUsername("testuser")).thenReturn(Optional.of(user));

        Authentication authentication = new UsernamePasswordAuthenticationToken(
                "testuser",
                "password123",
                List.of(new SimpleGrantedAuthority("ROLE_USER"))
        );

        Map<String, String> profileUpdate = Map.of(
                "firstName", "NewFirst",
                "lastName", "NewLast"
        );

        mockMvc.perform(put("/api/auth/profile")
                .principal(authentication)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(profileUpdate)))
                .andExpect(status().isOk())
                .andExpect(content().string("Profile updated successfully."));

        verify(userRepository).save(user);
        assertEquals("NewFirst", user.getFirstName());
        assertEquals("NewLast", user.getLastName());
        verifyNoInteractions(userAuthCacheService);
    }

    @Test
    void updateSecurityQuestions_shouldPreserveExistingAnswerHashWhenAnswerIsBlank() throws Exception {
        User user = new User();
        user.setId(UUID.randomUUID());
        user.setUsername("testuser");

        when(userRepository.findByUsername("testuser")).thenReturn(Optional.of(user));

        UserSecurityQuestion q1 = UserSecurityQuestion.builder()
                .id(UUID.randomUUID())
                .user(user)
                .question("What was the name of your first pet?")
                .answerHash("preserved-pet-hash")
                .build();
        UserSecurityQuestion q2 = UserSecurityQuestion.builder()
                .id(UUID.randomUUID())
                .user(user)
                .question("What is your mother's maiden name?")
                .answerHash("preserved-maiden-hash")
                .build();
        UserSecurityQuestion q3 = UserSecurityQuestion.builder()
                .id(UUID.randomUUID())
                .user(user)
                .question("What city were you born in?")
                .answerHash("preserved-city-hash")
                .build();

        List<UserSecurityQuestion> existing = List.of(q1, q2, q3);
        when(userSecurityQuestionRepository.findByUserId(user.getId())).thenReturn(existing);
        when(passwordEncoder.encode("chicago")).thenReturn("new-encoded-city-hash");

        Authentication authentication = new UsernamePasswordAuthenticationToken(
                "testuser",
                "password123",
                List.of(new SimpleGrantedAuthority("ROLE_USER"))
        );

        RegistrationRequest.SecurityQuestionDto dto1 = new RegistrationRequest.SecurityQuestionDto();
        dto1.setQuestion("What was the name of your first pet?");
        dto1.setAnswer(""); // blank - should preserve

        RegistrationRequest.SecurityQuestionDto dto2 = new RegistrationRequest.SecurityQuestionDto();
        dto2.setQuestion("What is your mother's maiden name?");
        dto2.setAnswer("   "); // whitespace - should preserve

        RegistrationRequest.SecurityQuestionDto dto3 = new RegistrationRequest.SecurityQuestionDto();
        dto3.setQuestion("What city were you born in?");
        dto3.setAnswer(" Chicago "); // new answer - should encode

        List<RegistrationRequest.SecurityQuestionDto> requestDto = List.of(dto1, dto2, dto3);

        mockMvc.perform(put("/api/auth/security-questions")
                .principal(authentication)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(requestDto)))
                .andExpect(status().isOk())
                .andExpect(content().string("Security questions updated successfully."));

        verify(userSecurityQuestionRepository).deleteAll(existing);

        @SuppressWarnings("unchecked")
        ArgumentCaptor<List<UserSecurityQuestion>> captor = ArgumentCaptor.forClass(List.class);
        verify(userSecurityQuestionRepository).saveAll(captor.capture());

        List<UserSecurityQuestion> saved = captor.getValue();
        assertEquals(3, saved.size());
        assertEquals("preserved-pet-hash", saved.get(0).getAnswerHash());
        assertEquals("preserved-maiden-hash", saved.get(1).getAnswerHash());
        assertEquals("new-encoded-city-hash", saved.get(2).getAnswerHash());
        verifyNoInteractions(userAuthCacheService);
    }

    @Test
    void updateSecurityQuestions_shouldFailWhenAnswerIsBlankForNewQuestion() throws Exception {
        User user = new User();
        user.setId(UUID.randomUUID());
        user.setUsername("testuser");

        when(userRepository.findByUsername("testuser")).thenReturn(Optional.of(user));
        when(userSecurityQuestionRepository.findByUserId(user.getId())).thenReturn(List.of());

        Authentication authentication = new UsernamePasswordAuthenticationToken(
                "testuser",
                "password123",
                List.of(new SimpleGrantedAuthority("ROLE_USER"))
        );

        RegistrationRequest.SecurityQuestionDto dto1 = new RegistrationRequest.SecurityQuestionDto();
        dto1.setQuestion("What was the name of your first pet?");
        dto1.setAnswer("");

        RegistrationRequest.SecurityQuestionDto dto2 = new RegistrationRequest.SecurityQuestionDto();
        dto2.setQuestion("What is your mother's maiden name?");
        dto2.setAnswer("Smith");

        RegistrationRequest.SecurityQuestionDto dto3 = new RegistrationRequest.SecurityQuestionDto();
        dto3.setQuestion("What city were you born in?");
        dto3.setAnswer("Dallas");

        List<RegistrationRequest.SecurityQuestionDto> requestDto = List.of(dto1, dto2, dto3);

        mockMvc.perform(put("/api/auth/security-questions")
                .principal(authentication)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(requestDto)))
                .andExpect(status().isBadRequest())
                .andExpect(content().string(containsString("Answer is required for new security question: What was the name of your first pet?")));
    }

    @Test
    void registerUser_shouldSucceedWithSecurityQuestions() throws Exception {
        when(userRepository.findByUsername("newuser")).thenReturn(Optional.empty());
        when(passwordEncoder.encode(any())).thenReturn("encoded-secret");

        String json = """
            {
                "username": "newuser",
                "password": "Secure-Phrase987!",
                "firstName": "John",
                "lastName": "Doe",
                "securityQuestions": [
                    { "question": "What was the name of your first pet?", "answer": "Fluffy" },
                    { "question": "What is your mother's maiden name?", "answer": "Smith" },
                    { "question": "What city were you born in?", "answer": "Dallas" }
                ]
            }
            """;

        mockMvc.perform(post("/api/auth/register")
                .contentType(MediaType.APPLICATION_JSON)
                .content(json))
                .andExpect(status().isOk())
                .andExpect(content().string("User registered successfully"));

        verify(userRepository).save(any(User.class));
    }

    @Test
    void registerUser_shouldSucceedWithQuestionsJsonAlias() throws Exception {
        when(userRepository.findByUsername("aliasuser")).thenReturn(Optional.empty());
        when(passwordEncoder.encode(any())).thenReturn("encoded-secret");

        String json = """
            {
                "username": "aliasuser",
                "password": "Secure-Phrase987!",
                "firstName": "Jane",
                "lastName": "Smith",
                "questions": [
                    { "question": "What was the name of your first pet?", "answer": "Spot" },
                    { "question": "What is your mother's maiden name?", "answer": "Johnson" },
                    { "question": "What city were you born in?", "answer": "Austin" }
                ]
            }
            """;

        mockMvc.perform(post("/api/auth/register")
                .contentType(MediaType.APPLICATION_JSON)
                .content(json))
                .andExpect(status().isOk())
                .andExpect(content().string("User registered successfully"));

        verify(userRepository).save(any(User.class));
    }

    @Test
    void registerUser_shouldFailWhenFewerThanThreeQuestionsProvided() throws Exception {
        when(userRepository.findByUsername("shortuser")).thenReturn(Optional.empty());

        String json = """
            {
                "username": "shortuser",
                "password": "Secure-Phrase987!",
                "firstName": "Jane",
                "lastName": "Smith",
                "securityQuestions": [
                    { "question": "What was the name of your first pet?", "answer": "Spot" }
                ]
            }
            """;

        mockMvc.perform(post("/api/auth/register")
                .contentType(MediaType.APPLICATION_JSON)
                .content(json))
                .andExpect(status().isBadRequest())
                .andExpect(content().string("Error: 3 security questions are required."));
    }

    @Test
    void registerUser_shouldFailWhenUsernameAlreadyExists() throws Exception {
        when(userRepository.findByUsername("existinguser")).thenReturn(Optional.of(new User()));

        String json = """
            {
                "username": "existinguser",
                "password": "Secure-Phrase987!",
                "firstName": "Jane",
                "lastName": "Smith",
                "securityQuestions": [
                    { "question": "What was the name of your first pet?", "answer": "Spot" },
                    { "question": "What is your mother's maiden name?", "answer": "Johnson" },
                    { "question": "What city were you born in?", "answer": "Austin" }
                ]
            }
            """;

        mockMvc.perform(post("/api/auth/register")
                .contentType(MediaType.APPLICATION_JSON)
                .content(json))
                .andExpect(status().isBadRequest())
                .andExpect(content().string("Error: Username is already taken!"));
    }
}
