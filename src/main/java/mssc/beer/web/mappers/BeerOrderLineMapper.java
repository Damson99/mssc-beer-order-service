package mssc.beer.web.mappers;

import mssc.beer.domain.BeerOrderLine;
import mssc.model.BeerOrderLineDto;
import org.mapstruct.DecoratedWith;
import org.mapstruct.Mapper;

@Mapper(uses = DateMapper.class)
@DecoratedWith(BeerOrderLineMapperDecorator.class)
public interface BeerOrderLineMapper
{
    BeerOrderLine dtoToBeerOrderLine(BeerOrderLineDto beerOrderLineDto);

    BeerOrderLineDto beerOrderLineToDto(BeerOrderLine beerOrderLine);
}
