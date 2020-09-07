package mssc.beer.services.testcomponents;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import mssc.beer.config.JmsConfig;
import mssc.model.event.ValidateOrderRequest;
import mssc.model.event.ValidateOrderResult;
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
    public void listen(Message<ValidateOrderRequest> message)
    {
        ValidateOrderRequest result = message.getPayload();
        boolean isValid = true;

        if(result.getBeerOrderDto().getCustomerRef() != null && result.getBeerOrderDto().getCustomerRef().equals("validation-failed"))
            isValid = false;

        jmsTemplate.convertAndSend(JmsConfig.VALIDATE_ORDER_RESPONSE_QUEUE,
                ValidateOrderResult.builder()
                .isValid(isValid)
                .orderId(result.getBeerOrderDto().getId())
                .build());
    }
}
