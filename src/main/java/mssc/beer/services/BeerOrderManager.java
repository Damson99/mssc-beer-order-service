package mssc.beer.services;

import mssc.beer.domain.BeerOrder;

import java.util.UUID;

public interface BeerOrderManager
{
    BeerOrder newBeerOrder(BeerOrder beerOrder);

    void processValidation(UUID id, Boolean isValid);
}
