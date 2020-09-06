package mssc.beer.services.testcomponents;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import mssc.beer.config.JmsConfig;
import mssc.model.event.ValidateBeerOrderRequest;
import mssc.model.event.ValidateOrderBeerResult;
import org.springframework.jms.annotation.JmsListener;
import org.springframework.jms.core.JmsTemplate;
import org.springframework.messaging.Message;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class BeerOrderValidationListener
{
    private final JmsTemplate jmsTemplate;

    @JmsListener(destination = JmsConfig.VALIDATE_ORDER_QUEUE)
    public void listen(Message message)
    {
        ValidateBeerOrderRequest result = (ValidateBeerOrderRequest) message.getPayload();

        jmsTemplate.convertAndSend(JmsConfig.VALIDATE_ORDER_RESPONSE_QUEUE,
                ValidateOrderBeerResult.builder()
                .isValid(true)
                .id(result.getBeerOrderDto().getId())
                .build());
    }
}
