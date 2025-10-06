Farm business manager.

Java_Llamas_02: Cornelius Davis, Mattea Isley, Demetrius Johnson, Kaheel Rowe

Preliminary plan:

Model classes (data only)
ItemSale → name, qty, unitPrice, date
Service → name, scheduledFor, customer, price, paid
AnimalSale → type (enum), quantity, priceEach, source ("in-house" or breeder name), date

Managers (one per area)
InventoryManager → addItemSale(), listItemSalesTotal()
ServiceManager → scheduleService(), listServices(), recordServicePayment()
AnimalManager → recordAnimalSale(), listAnimalSales()

UI flow (JOptionPane)
Main menu (while loop) → 4 buttons:
Track Item Sales
Services (schedule & payments)
Animal Sales
Specialty Resales (same as Animal Sales, but source = breeder)
Exit
Each choice opens a sub-menu with simple actions (add / list / mark paid).
