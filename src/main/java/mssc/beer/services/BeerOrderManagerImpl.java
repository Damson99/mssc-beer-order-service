package mssc.beer.services;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import mssc.beer.domain.BeerOrder;
import mssc.beer.domain.BeerOrderEventEnum;
import mssc.beer.domain.BeerOrderStatusEnum;
import mssc.beer.repositories.BeerOrderRepository;
import mssc.beer.sm.BeerOrderStateChangeInterceptor;
import mssc.model.BeerOrderDto;
import org.springframework.messaging.Message;
import org.springframework.messaging.support.MessageBuilder;
import org.springframework.statemachine.StateMachine;
import org.springframework.statemachine.config.StateMachineFactory;
import org.springframework.statemachine.support.DefaultStateMachineContext;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import javax.persistence.EntityManager;
import java.util.Optional;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class BeerOrderManagerImpl implements BeerOrderManager
{
    public static final String ORDER_ID_HEADER = "ORDER_ID_HEADER";
    private final StateMachineFactory<BeerOrderStatusEnum, BeerOrderEventEnum> stateMachineFactory;
    private final BeerOrderRepository beerOrderRepository;
    private final BeerOrderStateChangeInterceptor interceptor;
    private final EntityManager entityManager;

    @Override
    @Transactional
    public BeerOrder newBeerOrder(BeerOrder beerOrder)
    {
        beerOrder.setId(null);
        beerOrder.setOrderStatus(BeerOrderStatusEnum.NEW);

        BeerOrder savedBeerOrder = beerOrderRepository.saveAndFlush(beerOrder);
        sendBeerOrderEvent(savedBeerOrder, BeerOrderEventEnum.VALIDATE_ORDER);
        return savedBeerOrder;
    }

    @Override
    @Transactional
    public void processValidation(UUID id, Boolean isValid)
    {
        log.debug("Process Validation Result for beerOrderId: " + id + " Valid? " + isValid);
        entityManager.flush();

        beerOrderRepository.findById(id).ifPresentOrElse(beerOrder ->
        {
            if(isValid)
            {
                sendBeerOrderEvent(beerOrder, BeerOrderEventEnum.VALIDATION_PASSED);

                BeerOrder validatedOrder = beerOrderRepository.findById(beerOrder.getId()).get();
                sendBeerOrderEvent(validatedOrder, BeerOrderEventEnum.ALLOCATE_ORDER);
            }
            else
                sendBeerOrderEvent(beerOrder, BeerOrderEventEnum.VALIDATION_FAILED);
        }, () -> log.error("Order not found. Id: " + id));
    }

    @Override
    public void cancelOrder(UUID id)
    {
        beerOrderRepository.findById(id).ifPresentOrElse(beerOrder ->
        {
            sendBeerOrderEvent(beerOrder, BeerOrderEventEnum.CANCEL_ORDER);
        }, () -> log.error("Order not found. OrderId: " + id));
    }

    @Override
    public void beerOrderAllocationPassed(BeerOrderDto beerOrderDto)
    {
        beerOrderRepository.findById(beerOrderDto.getId()).ifPresentOrElse(beerOrder ->
        {
            sendBeerOrderEvent(beerOrder, BeerOrderEventEnum.ALLOCATION_SUCCESS);
            updateAllocatedQuantity(beerOrderDto, beerOrder);
        }, () -> log.error("Order not found. Id: " + beerOrderDto.getId()));
    }

    @Override
    public void beerOrderPickedUp(UUID id)
    {
        beerOrderRepository.findById(id).ifPresentOrElse(beerOrder ->
        {
            sendBeerOrderEvent(beerOrder, BeerOrderEventEnum.BEER_ORDER_PICKED_UP);

        }, () -> log.error("Order not found. Id: " + id));
    }

    @Override
    public void beerOrderAllocationPendingInventory(BeerOrderDto beerOrderDto)
    {
        Optional<BeerOrder> beerOrderOptional = beerOrderRepository.findById(beerOrderDto.getId());
        beerOrderOptional.ifPresentOrElse(beerOrder ->
        {
            sendBeerOrderEvent(beerOrder, BeerOrderEventEnum.ALLOCATION_NO_INVENTORY);
            updateAllocatedQuantity(beerOrderDto, beerOrder);
        }, () -> log.error("Order not found. Id: " + beerOrderDto.getId()));
    }

    private void updateAllocatedQuantity(BeerOrderDto beerOrderDto, BeerOrder beerOrder)
    {
        Optional<BeerOrder> allocatedOrderOptional = beerOrderRepository.findById(beerOrderDto.getId());
        allocatedOrderOptional.ifPresentOrElse(allocatedOrder ->
        {
            allocatedOrder.getBeerOrderLines()
                    .forEach(beerOrderLine -> beerOrderDto.getBeerOrderLines().forEach(beerOrderLineDto ->
                    {
                        if(beerOrderDto.getId().equals(beerOrder.getId()))
                            beerOrderLine.setOrderQuantity(beerOrderLineDto.getOrderQuantity());
                    }));
            beerOrderRepository.saveAndFlush(allocatedOrder);
        }, () -> log.error("Order not found. Id: " + beerOrder.getId()));
    }

    @Override
    public void beerOrderAllocationFailed(BeerOrderDto beerOrderDto)
    {
        Optional<BeerOrder> beerOrderOptional = beerOrderRepository.findById(beerOrderDto.getId());
        beerOrderOptional.ifPresentOrElse(beerOrder ->
                sendBeerOrderEvent(beerOrder, BeerOrderEventEnum.ALLOCATION_FAILED),
                () -> log.error("Order not found. Id: " + beerOrderDto.getId()));
    }



    private void sendBeerOrderEvent(BeerOrder beerOrder, BeerOrderEventEnum eventEnum)
    {
        StateMachine<BeerOrderStatusEnum, BeerOrderEventEnum> stateMachine = build(beerOrder);
        Message message = MessageBuilder
                .withPayload(eventEnum)
                .setHeader("ORDER_ID_HEADER", beerOrder.getId().toString())
                .build();
        stateMachine.sendEvent(message);
    }



    private StateMachine<BeerOrderStatusEnum, BeerOrderEventEnum> build(BeerOrder beerOrder)
    {
        StateMachine<BeerOrderStatusEnum, BeerOrderEventEnum> stateMachine = stateMachineFactory.getStateMachine(beerOrder.getId());

        stateMachine.stop();
        stateMachine.getStateMachineAccessor()
                .doWithAllRegions(stateMachineAccessor ->
                {
                    stateMachineAccessor.addStateMachineInterceptor(interceptor);
                    stateMachineAccessor.resetStateMachine(new DefaultStateMachineContext<>(beerOrder.getOrderStatus(),
                            null, null, null));
                });
        stateMachine.start();

        return stateMachine;
    }
}
