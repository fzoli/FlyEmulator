A programot írta: Farkas Zoltán - DZ54IQ

Rendszerigény:
- Minimum JRE6
- Operációs rendszer: Windows, Linux, Mac
- OpenGL támogatás (egyeznie kell a JRE és a driver architektúrájának)
- Hangkártya

A program elindítható paraméterek megadása nélkül illetve 3 paraméterrel is.
Paraméter nélkül 640x480-as felbontás és anaglif üzemmód lép érvénybe.

Paraméterek:
1. ablak szélessége (integer)
2. ablak magassága (integer)
3. anaglif opció ki/be (boolean: true, false, 0, 1)

példa (dist könyvtáron belül):
java -jar FlyEmulator.jar 1024 768 true

Irányítás:
FEL/LE NYÍL: repülőgép magasságának állítása
SZÓKÖZ: bomba ledobása/új bomba
Q: alaphelyzet
Á: meggyulladás a nyomás pillanatára (eredetileg teszt volt, de nem vettem ki)
