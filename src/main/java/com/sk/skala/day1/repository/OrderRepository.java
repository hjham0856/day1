package com.sk.skala.day1.repository;

public interface OrderRepository extends JpaRepogitory<Order,Long>{
    Optional<Order> findByIdAndOwnerId(String id, String ownerId);

    List<Order> findTop5ByOwnerIdOrderByOrderedDesc(String ownerId);

    @Query("select o from Order o where o.ownerId = :ownerId" + " and o.status in :statuses")
    List<Order> findActive(@Param("ownerId") String ownerId, @Param("statuses") List<OrderStatus> statuses);
    
}

@Repository
public class ShippingApiRepository{
    private final RestClient restClient;
    //public Optional<Tracking> findTracking(String invoiceNo){...}
}
