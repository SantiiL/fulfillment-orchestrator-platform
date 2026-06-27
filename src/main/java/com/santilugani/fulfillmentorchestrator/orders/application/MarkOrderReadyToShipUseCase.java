package com.santilugani.fulfillmentorchestrator.orders.application;

public interface MarkOrderReadyToShipUseCase {

    OrderResult markOrderReadyToShip(MarkOrderReadyToShipCommand command);
}
