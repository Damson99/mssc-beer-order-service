package mssc.beer.sm;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import mssc.beer.domain.BeerOrder;
import mssc.beer.domain.BeerOrderEventEnum;
import mssc.beer.domain.BeerOrderStatusEnum;
import mssc.beer.repositories.BeerOrderRepository;
import mssc.beer.services.BeerOrderManagerImpl;
import org.springframework.messaging.Message;
import org.springframework.statemachine.StateMachine;
import org.springframework.statemachine.state.State;
import org.springframework.statemachine.support.StateMachineInterceptorAdapter;
import org.springframework.statemachine.transition.Transition;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.Optional;
import java.util.UUID;

@Slf4j
@Component
@RequiredArgsConstructor
public class BeerOrderStateChangeInterceptor extends StateMachineInterceptorAdapter<BeerOrderStatusEnum, BeerOrderEventEnum>
{
    private final BeerOrderRepository beerOrderRepository;

    @Override
    @Transactional
    public void postStateChange(State<BeerOrderStatusEnum, BeerOrderEventEnum> state, Message<BeerOrderEventEnum> message,
                                Transition<BeerOrderStatusEnum, BeerOrderEventEnum> transition, StateMachine<BeerOrderStatusEnum, BeerOrderEventEnum> stateMachine)
    {
        Optional.ofNullable(message)
                .flatMap(msg -> Optional.ofNullable((String) msg.getHeaders().getOrDefault(BeerOrderManagerImpl.ORDER_ID_HEADER, " ")))
                    .ifPresent(beerOrderId ->
                    {
                        log.debug("Saving state for order id: " + beerOrderId + "Status:" + state.getId());
                        BeerOrder beerOrder = beerOrderRepository.getOne(UUID.fromString(beerOrderId));
                        beerOrder.setOrderStatus(state.getId());
                        beerOrderRepository.saveAndFlush(beerOrder);
                    });
    }
}
