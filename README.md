# My Market (Fabric 1.21.1)

Money is a plain number (not an item). Every item is worth money by rarity.

## Commands / คำสั่ง
- /sell   : open the sell window / เปิดหน้าต่างขาย (คลิกของในกระเป๋าเพื่อขายทันที)
- /shop   : open the buy window (also /buy) / เปิดหน้าต่างซื้อของทุกอย่าง
- /money  : show your money (also /balance) / ดูเงิน
- /price  : price of the item in your hand / ดูราคาของที่ถืออยู่
- /money give <player> <amount> and /money take ... : admin only / สำหรับแอดมิน (ต้องเปิดโกง)

## Build / วิธี build
1. Install JDK 21.
2. In this folder run:  ./gradlew build   (Windows: gradlew.bat build)
3. Your mod file is in build/libs/mymarket-1.0.0.jar  (do NOT use the "-sources" file)
4. Put it in the mods folder together with Fabric API for 1.21.1.

## Change prices / แก้ราคา
Edit config/mymarket_prices.json (created on first run). Example:
{ "minecraft:diamond": 200, "minecraft:dirt": 0 }
0 = cannot be sold or bought. Restart the world to apply.
