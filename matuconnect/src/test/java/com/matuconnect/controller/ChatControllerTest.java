package com.matuconnect.controller;

import org.springframework.context.annotation.Import;
import com.matuconnect.security.SecurityConfig;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.messages.Message;
import org.springframework.ai.chat.messages.MessageType;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(ChatController.class)
@Import(SecurityConfig.class)
class ChatControllerTest {

    @Autowired
    MockMvc mockMvc;

    @MockitoBean
    ChatClient chatClient;

    private ChatClient.ChatClientRequestSpec stubChatClient(String reply) {
        ChatClient.ChatClientRequestSpec requestSpec = mock(ChatClient.ChatClientRequestSpec.class);
        ChatClient.CallResponseSpec callSpec = mock(ChatClient.CallResponseSpec.class);
        when(chatClient.prompt()).thenReturn(requestSpec);
        when(requestSpec.messages(anyList())).thenReturn(requestSpec);
        when(requestSpec.call()).thenReturn(callSpec);
        when(callSpec.content()).thenReturn(reply);
        return requestSpec;
    }

    @SuppressWarnings("unchecked")
    private List<Message> capturedMessages(ChatClient.ChatClientRequestSpec requestSpec) {
        ArgumentCaptor<List<Message>> captor = ArgumentCaptor.forClass(List.class);
        verify(requestSpec).messages(captor.capture());
        return captor.getValue();
    }

    @Test
    void returnsAgentReplyForAMessage() throws Exception {
        stubChatClient("Board route 46 at Kencom heading west.");

        mockMvc.perform(post("/api/chat").with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"message\":\"How do I get to Westlands from Kencom?\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.reply").value("Board route 46 at Kencom heading west."));
    }

    @Test
    void sendsOnlyTheNewMessageWhenNoHistoryIsSupplied() throws Exception {
        ChatClient.ChatClientRequestSpec requestSpec = stubChatClient("ok");

        mockMvc.perform(post("/api/chat").with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"message\":\"Hello\"}"))
                .andExpect(status().isOk());

        List<Message> sent = capturedMessages(requestSpec);
        assertThat(sent).hasSize(1);
        assertThat(sent.getFirst().getMessageType()).isEqualTo(MessageType.USER);
        assertThat(sent.getFirst().getText()).isEqualTo("Hello");
    }

    @Test
    void replaysPriorTurnsSoTheAgentCanResolveBackReferences() throws Exception {
        ChatClient.ChatClientRequestSpec requestSpec = stubChatClient("Taking the first option.");

        mockMvc.perform(post("/api/chat").with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "message": "the first one please",
                                  "history": [
                                    {"role": "user", "content": "How do I get to Limuru?"},
                                    {"role": "assistant", "content": "Which Ngara stop do you mean?"}
                                  ]
                                }
                                """))
                .andExpect(status().isOk());

        List<Message> sent = capturedMessages(requestSpec);
        assertThat(sent).hasSize(3);
        assertThat(sent.get(0).getMessageType()).isEqualTo(MessageType.USER);
        assertThat(sent.get(0).getText()).isEqualTo("How do I get to Limuru?");
        assertThat(sent.get(1).getMessageType()).isEqualTo(MessageType.ASSISTANT);
        assertThat(sent.get(1).getText()).isEqualTo("Which Ngara stop do you mean?");
        assertThat(sent.get(2).getText()).isEqualTo("the first one please");
    }

    @Test
    void ignoresHistoryTurnsThatAreNotUserOrAssistant() throws Exception {
        ChatClient.ChatClientRequestSpec requestSpec = stubChatClient("ok");

        // The frontend renders local-only "error" bubbles; those are UI state,
        // not something the agent ever said, so they must not be replayed.
        mockMvc.perform(post("/api/chat").with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "message": "retry",
                                  "history": [
                                    {"role": "error", "content": "Something went wrong."},
                                    {"role": "user", "content": "earlier question"}
                                  ]
                                }
                                """))
                .andExpect(status().isOk());

        List<Message> sent = capturedMessages(requestSpec);
        assertThat(sent).hasSize(2);
        assertThat(sent.get(0).getText()).isEqualTo("earlier question");
        assertThat(sent.get(1).getText()).isEqualTo("retry");
    }

    @Test
    void returnsBadRequestWhenBodyIsMissing() throws Exception {
        mockMvc.perform(post("/api/chat").with(csrf()).contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isBadRequest());
    }
}
