# nova-java-27-cqrs

La capacidad de CQRS de Nova Platform. Un servicio separa lo que cambia el estado, los **comandos**, de
lo que lo lee, las **consultas**, y los ejecuta a través de un `CommandBus` y un `QueryBus`. Cada mensaje
pasa por una cadena de comportamientos donde vive lo transversal: la observación, la auditoría, la
autorización, la validación y la transacción. Un caso de uso nuevo es un record y un handler.

Las decisiones están en [ADR-053](https://github.com/ahincho/nova-shared-01-docs/blob/main/adrs/shared/ADR-053-cqrs-con-command-bus-y-query-bus.md),
y la forma del repositorio —un contrato y sus implementaciones juntos, con una sola versión— en
[ADR-041](https://github.com/ahincho/nova-shared-01-docs/blob/main/adrs/java/ADR-041-un-repositorio-por-capacidad.md).

## Módulos

| Módulo | `groupId` | Qué es |
|---|---|---|
| `nova-cqrs` | `pe.edu.nova.java.libs` | los contratos, los dos buses, la cadena de comportamientos, la auditoría y la autorización; Java puro, sin Spring ni ningún otro framework |
| `nova-cqrs-spring-boot-starter` | `pe.edu.nova.java.starters` | registra los buses con los handlers que son beans, y los cinco comportamientos de Nova, bajo `nova.cqrs.*` |

Los dos se publican en `https://maven.pkg.github.com/ahincho/nova-java-27-cqrs` con la misma versión.

## Desde la 1.0.0, la API es estable

La 0.1.0 salió sin consumidor. La validó el servicio de pedidos de Plaza
([`nova-plaza-03-spring-boot-orders`](https://github.com/ahincho/nova-plaza-03-spring-boot-orders)), que
ejecuta su compra y sus dos consultas por los buses, y con eso la capacidad pasó a la 1.0.0. Desde aquí
un cambio incompatible de la API, o del orden de los comportamientos, es una versión mayor. Con la 1.0.0
el starter cumple la condición de ADR-052 para entrar en el meta-starter de Nova: sin un handler, los
buses no hacen nada.

## Cómo se usa

Un mensaje es un record tipado por su resultado, y su handler es una clase que lo recibe:

```java
public record PlaceOrder(@NotBlank String customerId, @NotEmpty List<Line> lines) implements Command<UUID> {}

public record FindOrder(String customerId, UUID id) implements Query<OrderView> {}

@Component
class PlaceOrderHandler implements CommandHandler<PlaceOrder, UUID> {
    @Override
    public UUID handle(PlaceOrder command) {
        // crea el pedido y devuelve su identificador
    }
}
```

El controlador depende solo de los buses, y el resultado sale del tipo del mensaje, sin cast:

```java
UUID id = commandBus.execute(new PlaceOrder(customerId, lines));
OrderView order = queryBus.execute(new FindOrder(customerId, id));
```

`nova-architecture-rules` 1.2.0 deja que un controlador use `CommandBus` y `QueryBus`, porque viven bajo
`pe.edu.nova.java.libs..`.

### Las reglas

- **Un tipo de mensaje tiene exactamente un handler.** Dos handlers para el mismo tipo impiden el
  arranque, con `DuplicateHandlerException`.
- **Un mensaje encuentra a su handler por su clase exacta**, sin herencia. El tipo de mensaje es una clase
  concreta, como un record.
- **Un mensaje sin handler** lanza `HandlerNotFoundException`, antes de pasar por los comportamientos. Es
  un defecto del servicio: el estándar de API lo responde como un 500 sin el detalle, que va al log.
- **Lo que lanza el handler sale tal cual.** Un `DomainError` llega al manejo de errores de ADR-031 con su
  capa y su código.
- **Un handler no conoce el bus.** Se prueba con un `new` y una llamada, y si dependiera del bus, Spring
  no podría crearlo.
- **Los buses son síncronos** y corren en el hilo de quien llama.

### El tipo de mensaje de cada handler

El starter lo lee de los genéricos: de la clase de un `@Component`, o del tipo que devuelve un método
`@Bean`, así que una lambda también sirve si el método lo declara completo:

```java
@Bean
QueryHandler<FindOrder, OrderView> findOrderHandler(OrderRepository orders) {
    return query -> orders.view(query.customerId(), query.id());
}
```

Un handler con `@Transactional` u otro proxy también funciona: se mira su clase real. Uno sin genéricos
impide el arranque y el error dice qué bean es. Sin Spring, `HandlerRegistry.builder()` registra cada
handler, con su tipo o leyéndolo de sus genéricos.

## Los comportamientos

El starter los registra en este orden, de afuera hacia adentro. El orden es parte del contrato: cambiarlo
es una versión mayor.

| Orden | Comportamiento | Comandos | Consultas | Qué hace |
|---|---|---|---|---|
| 100 | `ObservationBehavior` | sí | sí | una `Observation` de Micrometer: el timer `nova.cqrs.command` o `nova.cqrs.query`, con las etiquetas `message` y `outcome`, y un span |
| 200 | `AuditBehavior` | sí | sí | registra quién, qué tipo, cuándo, cuánto tardó y cómo terminó, incluido un rechazo |
| 300 | `AuthorizationBehavior` | sí | sí | pregunta a la `AccessPolicy`; si no permite, 403 `FORBIDDEN` |
| 400 | `ValidationBehavior` | sí | sí | Bean Validation sobre el record; si falla, 400 `INVALID_INPUT` con un error por campo |
| 500 | `TransactionBehavior` | sí, de lectura y escritura | sí, de solo lectura | `TransactionTemplate` con propagación `REQUIRED` |

- **La auditoría va antes que la autorización** para que un intento rechazado quede registrado como
  `DENIED`.
- **La transacción va adentro de todo** y, con `REQUIRED`, se suma a una que ya exista: la que abre la
  idempotencia alrededor del controlador, por ejemplo. Una excepción del handler la deshace.
- **Cada uno se registra solo si su pieza existe:** la observación si hay Micrometer, la validación si hay
  Bean Validation y la transacción si hay transacciones de Spring. Sin un `Validator`, un
  `ObservationRegistry` o un `PlatformTransactionManager` en el contexto, el mensaje pasa sin ellos.
- **Un comportamiento propio** es un bean que implementa `CommandBehavior`, `QueryBehavior` o los dos, con
  `@Order`. Con `@Order(350)` corre después de la autorización y antes de la validación.
  `NovaCqrsBehaviorOrder` tiene los números de Nova.

### La auditoría

Cada registro es un `AuditRecord`: el tipo de mensaje, comando o consulta, el actor, el instante, la
duración, el resultado (`SUCCEEDED`, `DENIED` o `FAILED`) y, si falló, el código del error. **No lleva el
contenido del mensaje ni su resultado**, que pueden tener datos personales, ni el texto de una excepción.

El destino por defecto es una línea en el logger `nova.audit`, con cada campo también como par
clave-valor para un log estructurado:

```
COMMAND PlaceOrder by customer-7: SUCCEEDED - in 12 ms
```

Si el destino falla, el resultado del mensaje no cambia, y la falla se escribe en el log como un error.

### Los puertos

| Puerto | Qué decide | Por defecto |
|---|---|---|
| `ActorResolver` | quién ejecuta el mensaje | el nombre de la autenticación de Spring Security, si el servicio la usa; si no, nadie |
| `AccessPolicy` | si un actor puede ejecutar un mensaje | permitir todo |
| `AuditSink` | dónde va cada registro | `LoggingAuditSink`, el logger `nova.audit` |

Un servicio reemplaza cualquiera declarando su bean. Uno que recibe la identidad de un BFF en un header,
como los pedidos de Plaza con `X-Customer-Id`, declara su `ActorResolver`. Los roles de Keycloak entran
como una `AccessPolicy` que lee los roles del token. La auditoría a Kafka o a MongoDB es un `AuditSink`.

### Las propiedades

| Propiedad | Por defecto | Qué hace |
|---|---|---|
| `nova.cqrs.enabled` | `true` | registra los buses |
| `nova.cqrs.observation.enabled` | `true` | la observación |
| `nova.cqrs.audit.enabled` | `true` | la auditoría |
| `nova.cqrs.audit.queries` | `true` | si la auditoría alcanza a las consultas |
| `nova.cqrs.authorization.enabled` | `true` | la autorización |
| `nova.cqrs.validation.enabled` | `true` | la validación |
| `nova.cqrs.transaction.enabled` | `true` | la transacción |

## Los eventos de dominio

`nova-cqrs` no trae un `EventBus`: uno en memoria pierde el evento si el proceso cae entre el commit y la
entrega. Mientras no exista la capacidad de outbox, ADR-053 dice cómo manejarlos:

- **Dentro del mismo servicio:** el agregado registra el evento con `AbstractAggregateRoot` de Spring Data,
  y quien reacciona usa `@TransactionalEventListener(phase = AFTER_COMMIT)`. Si la reacción cambia estado,
  ejecuta un comando por el bus, en su propia transacción.
- **Hacia otro servicio:** la capacidad de outbox, que guarda el evento en la misma transacción que el
  cambio.

## Instalación

```kotlin
repositories {
    maven {
        url = uri("https://maven.pkg.github.com/ahincho/nova-java-27-cqrs")
        credentials {
            username = System.getenv("GITHUB_ACTOR")
            password = System.getenv("GITHUB_TOKEN")
        }
    }
}

dependencies {
    implementation("pe.edu.nova.java.starters:nova-cqrs-spring-boot-starter:1.0.0")
}
```

El starter trae `nova-cqrs` y `nova-api-standard` 1.1.0. Spring Boot, Micrometer, Bean Validation y
Spring Security los trae el servicio.

## Desarrollo

```bash
./gradlew build
```

Corre Spotless, Checkstyle, las pruebas con la cobertura mínima de 80 % y el Javadoc. El build pide un
`GITHUB_TOKEN` con `read:packages` para el toolchain y `nova-api-standard`.

| Módulo | Pruebas |
|---|---|
| `nova-cqrs` | los buses y su cadena, el registro y sus reglas, la auditoría, la autorización, y que el núcleo no dependa de ningún framework |
| `nova-cqrs-spring-boot-starter` | el contexto de Spring Boot con `ApplicationContextRunner`: los handlers como beans y detrás de un proxy, el orden de los comportamientos, cada uno encendido y apagado, la transacción que se suma a otra, la observación y el actor de Spring Security |

## Licencia

Eclipse Public License 2.0. Ver [LICENSE](LICENSE).
