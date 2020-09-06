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

    @Override
    @Transactional
    public BeerOrder newBeerOrder(BeerOrder beerOrder)
    {
        beerOrder.setId(null);
        beerOrder.setOrderStatus(BeerOrderStatusEnum.NEW);

        BeerOrder savedBeerOrder = beerOrderRepository.save(beerOrder);
        sendBeerOrderEvent(savedBeerOrder, BeerOrderEventEnum.VALIDATE_ORDER);
        return savedBeerOrder;
    }

    @Override
    public void processValidation(UUID id, Boolean isValid)
    {
        Optional<BeerOrder> beerOrderOptional = beerOrderRepository.findById(id);
        beerOrderOptional.ifPresentOrElse(beerOrder ->
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
    public void beerOrderAllocationPassed(BeerOrderDto beerOrderDto)
    {
        Optional<BeerOrder> beerOrderOptional = beerOrderRepository.findById(beerOrderDto.getId());
        beerOrderOptional.ifPresentOrElse(beerOrder ->
        {
            sendBeerOrderEvent(beerOrder, BeerOrderEventEnum.ALLOCATION_SUCCESS);
            updateAllocateQuantity(beerOrderDto, beerOrder);
        }, () -> log.error("Order not found. Id: " + beerOrderDto.getId()));
    }

    @Override
    public void beerOrderAllocationPendingInventory(BeerOrderDto beerOrderDto)
    {
        Optional<BeerOrder> beerOrderOptional = beerOrderRepository.findById(beerOrderDto.getId());
        beerOrderOptional.ifPresentOrElse(beerOrder ->
        {
            sendBeerOrderEvent(beerOrder, BeerOrderEventEnum.ALLOCATION_NO_INVENTORY);
            updateAllocateQuantity(beerOrderDto, beerOrder);
        }, () -> log.error("Order not found. Id: " + beerOrderDto.getId()));
    }

    private void updateAllocateQuantity(BeerOrderDto beerOrderDto, BeerOrder beerOrder)
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
            beerOrderRepository.saveAndFlush(beerOrder);
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
