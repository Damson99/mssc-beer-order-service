package mssc.model.event;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import mssc.model.BeerOrderDto;

@Data
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class AllocationOrderResult
{
    private BeerOrderDto beerOrderDto;
    private Boolean allocationError = false;
    private Boolean pendingInventory = false;
}
