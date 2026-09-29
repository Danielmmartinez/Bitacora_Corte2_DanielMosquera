package edu.dosw.restaurante.persistence.document;

import edu.dosw.restaurante.model.domain.EstadoPedido;
import edu.dosw.restaurante.model.domain.TipoEventoPedido;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.index.Indexed;
import org.springframework.data.mongodb.core.mapping.Document;

import java.time.LocalDateTime;
import java.util.Map;

/**
 * Documento de la colección "eventos_pedido" en MongoDB. Es el equivalente no relacional
 * de una @Entity: describe cómo se guarda un EventoPedido, sin lógica de negocio.
 */
@Document(collection = "eventos_pedido")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class EventoPedidoDocument {

    @Id
    private String id; // Mongo genera un ObjectId (ej. "66f2a1b2c3d4e5f6a7b8c9d0") al insertar

    @Indexed // acelera la consulta del historial de un pedido
    private Long idPedido;

    private Long idMesa;
    private TipoEventoPedido tipo;           // Mongo guarda los enums como texto
    private EstadoPedido estadoAnterior;
    private EstadoPedido estadoNuevo;
    private LocalDateTime fecha;

    // Sub-documento libre: cada tipo de evento guarda campos distintos, sin cambiar ningún esquema
    private Map<String, Object> detalle;
}
