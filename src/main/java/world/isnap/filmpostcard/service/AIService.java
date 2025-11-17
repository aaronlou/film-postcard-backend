package world.isnap.filmpostcard.service;

import com.alibaba.cloud.ai.dashscope.chat.DashScopeChatModel;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.chat.messages.UserMessage;
import org.springframework.ai.chat.prompt.Prompt;
import org.springframework.stereotype.Service;

@Service
@Slf4j
@RequiredArgsConstructor
public class AIService {
    
    private final DashScopeChatModel chatModel;
    
    public String polishText(String text, String templateType) {
        String promptText = buildPrompt(text, templateType);
        
        try {
            Prompt prompt = new Prompt(new UserMessage(promptText));
            String response = chatModel.call(prompt)
                    .getResult()
                    .getOutput()
                    .getText();
            
            log.info("AI polished text successfully for template: {}", templateType);
            return response;
        } catch (Exception e) {
            log.error("Error calling AI service", e);
            throw new RuntimeException("AI service unavailable", e);
        }
    }
    
    private String buildPrompt(String text, String templateType) {
        String context = switch (templateType) {
            case "postcard" -> "这是一张明信片，需要温暖、简洁、充满情感的文字";
            case "bookmark" -> "这是一个书签，需要励志、优雅、富有哲理的文字";
            case "polaroid" -> "这是拍立得照片，需要随性、真实、记录瞬间的文字";
            case "greeting" -> "这是一张贺卡，需要祝福、温馨、正式的文字";
            default -> "这是一段需要优化的文字";
        };
        
        return String.format(
                "%s。请帮我优化以下文字，使其更加优美动人，保持原意但提升文学性和感染力。" +
                "请直接返回优化后的文字，不要添加任何额外说明或引号。\n\n原文：%s",
                context, text
        );
    }
}
