package guru.sfg.beer.order.service.web.mappers;

import guru.sfg.beer.order.service.domain.BeerOrderLine;
import guru.sfg.beer.order.service.services.beer.BeerService;
import guru.sfg.brewery.model.BeerDto;
import guru.sfg.brewery.model.BeerOrderLineDto;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;

import java.util.Optional;

@RequiredArgsConstructor
public abstract class BeerOrderLineMapperDecorator implements BeerOrderLineMapper {

    private final BeerService beerService;
    private final BeerOrderLineMapper beerOrderLineMapper;


    @Override
    public BeerOrderLineDto beerOrderLineToDto(BeerOrderLine line)
    {
        BeerOrderLineDto orderLineDto = beerOrderLineMapper.beerOrderLineToDto(line);
        Optional<BeerDto> beerDtoOptional = beerService.getBeerByUpc(line.getUpc());

        beerDtoOptional.ifPresent(beerDto ->
        {
            orderLineDto.setBeerName(beerDto.getBeerName());
            orderLineDto.setBeerStyle(beerDto.getBeerStyle());
            orderLineDto.setPrice(beerDto.getPrice());
            orderLineDto.setBeerId(beerDto.getId());
        });

        return orderLineDto;
    }
}
