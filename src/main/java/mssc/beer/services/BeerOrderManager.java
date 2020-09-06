package mssc.beer.services;

import mssc.beer.domain.BeerOrder;
import mssc.model.BeerOrderDto;

import java.util.UUID;

public interface BeerOrderManager
{
    BeerOrder newBeerOrder(BeerOrder beerOrder);

    void processValidation(UUID id, Boolean isValid);

    void beerOrderAllocationPassed(BeerOrderDto beerOrderDto);

    void beerOrderAllocationPendingInventory(BeerOrderDto beerOrderDto);

    void beerOrderAllocationFailed(BeerOrderDto beerOrderDto);
}
