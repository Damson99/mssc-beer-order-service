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
        ValidateOrderRequest request = message.getPayload();
        boolean isValid = true;
        boolean sendResponse = true;

        if(request.getBeerOrderDto().getCustomerRef() != null)
        {
            if(request.getBeerOrderDto().getCustomerRef().equals("validation-failed"))
                isValid = false;
            else if(request.getBeerOrderDto().getCustomerRef().equals("dont-validate"))
                sendResponse = false;
        }

        if(sendResponse)
        {
            jmsTemplate.convertAndSend(JmsConfig.VALIDATE_ORDER_RESPONSE_QUEUE,
                    ValidateOrderResult.builder()
                    .isValid(isValid)
                    .orderId(request.getBeerOrderDto().getId())
                    .build());
        }
    }
}
