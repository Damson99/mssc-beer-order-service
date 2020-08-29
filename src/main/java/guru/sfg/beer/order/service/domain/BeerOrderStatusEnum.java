package guru.sfg.beer.order.service.domain;

public enum BeerOrderStatusEnum
{
    NEW, VALIDATED, VALIDATED_EXCEPTION,
    ALLOCATED, ALLOCATION_EXCEPTION,
    PENDING_INVENTORY, PICKED_UP, DELIVERED, DELIVERY_EXCEPTION
}
