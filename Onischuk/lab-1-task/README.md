# HW1
Вы работаете в IT-отделе крупного промышленного предприятия. Ваша задача — разработать систему для моделирования и анализа работы производственных линий, на которых выпускаются различные изделия. Каждая линия может производить определённый тип продукции, и у каждой линии есть свои характеристики: эффективность, загрузка, тип изделия и т.д.

## 1. ООП + Generic

Создайте абстрактный класс Product (id: String, name: String, productionTime: int)
В нем абстрактный метод getCategory() -> возвращает строку категории изделия (Electronics, Mechanical, Chemical)
От данного класса унаследуйте ElectronicProduct, MechanicalProduct, ChemicalProduct... [не забудьте GET CATEGORY!]

Создайте абстрактный класс ProductionLine<T extends Product>:
String lineId
List<T> products -- что производится на данной линии
double efficiency (0.0 - 1.0)

и методы get для всех полей + boolean canProduce(Product product) -- может ли линия работать с данным типом продукта

Реализуйте конкретные линии:
```
ElectronicsLine extends ProductionLine<ElectronicProduct>, canProduce(product) = true если product instanceof ElectronicProduct, по аналогии и другие
```

* - *здесь можно за плюсик нагуглить как красиво сложить все по package*

## 2. Stream API (ИСПОЛЬЗУЙТЕ ЗДЕСЬ СТРИМАПИ)

Создать просто класс ProdAnalyzer с методами

List<ProductionLine<? extends Product>> getAllLines() -- все линии (храните список всех линий в поле класса)

---

- List<String> getHighEfficiencyLines(double threshold) -- вернуть ID линий с эффективностью выше threshold
- Map<String, Long> countProductsByCategory() -- подсчитать общее количество изделий по категориям
- Optional<ProductionLine<? extends Product>> findMostLoadedLine() -- линия с наибольшим количеством изделий в списке products
- List<Product> getAllProductsFromLines() -- собрать все изделия со всех линий в один список

---

## 3. Тестирование

Предлагаю вам самим выделить кейсы, которые можно протестировать, их здравость я уже сам оценю. В дальнейшем научимся писать тесты)

