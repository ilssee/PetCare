import assert from 'node:assert/strict';
import { describe, it } from 'node:test';
import {
  COMENTARIO_MAXIMO,
  MENSAJE_SIN_CALIFICACION,
  promedioRedondeado,
  validarResena,
} from '../../src/modules/resena/resena.logica';
import { agruparHorarioSemanal, agruparServicios } from '../../src/modules/mapa/mapa.perfil';
import { Dia, Horario } from '../../src/modules/mapa/mapa.types';

describe('reseñas (RF45)', () => {
  it('acepta una calificación de 1 a 5 con o sin comentario', () => {
    assert.deepEqual(validarResena({ calificacion: 5 }), { calificacion: 5, comentario: null });
    assert.deepEqual(validarResena({ calificacion: 1, comentario: '  Muy buena atención  ' }), {
      calificacion: 1,
      comentario: 'Muy buena atención',
    });
  });

  it('no deja guardar sin calificación', () => {
    assert.throws(() => validarResena({}), { message: MENSAJE_SIN_CALIFICACION });
    assert.throws(() => validarResena({ comentario: 'Hola' }), { message: MENSAJE_SIN_CALIFICACION });
    assert.throws(() => validarResena({ calificacion: '' }), { message: MENSAJE_SIN_CALIFICACION });
    assert.throws(() => validarResena({ calificacion: null }), { message: MENSAJE_SIN_CALIFICACION });
  });

  it('rechaza calificaciones fuera de rango o que no son enteros', () => {
    for (const mala of [0, 6, -1, 4.5, 'abc']) {
      assert.throws(() => validarResena({ calificacion: mala }), { message: 'La calificación debe ser un número de 1 a 5' });
    }
  });

  it('un comentario vacío o de solo espacios se guarda como sin comentario', () => {
    assert.equal(validarResena({ calificacion: 3, comentario: '   ' }).comentario, null);
    assert.equal(validarResena({ calificacion: 3, comentario: '' }).comentario, null);
  });

  it('limita el largo del comentario', () => {
    assert.equal(validarResena({ calificacion: 3, comentario: 'a'.repeat(COMENTARIO_MAXIMO) }).comentario?.length, COMENTARIO_MAXIMO);
    assert.throws(() => validarResena({ calificacion: 3, comentario: 'a'.repeat(COMENTARIO_MAXIMO + 1) }), {
      message: `El comentario no puede pasar de ${COMENTARIO_MAXIMO} caracteres`,
    });
  });

  it('calcula el promedio con un decimal, o nulo si no hay reseñas', () => {
    assert.equal(promedioRedondeado(14, 3), 4.7); // 5, 5 y 4
    assert.equal(promedioRedondeado(7, 2), 3.5); // 4 y 3
    assert.equal(promedioRedondeado(5, 1), 5);
    assert.equal(promedioRedondeado(0, 0), null);
  });
});

describe('perfil de la veterinaria (RF41)', () => {
  const franja = (dia: Dia, apertura: string, cierre: string): Horario => ({ dia, apertura, cierre });

  it('junta los días seguidos con el mismo horario y marca el de hoy', () => {
    // Lunes a viernes de 9 a 19, sábado de 9 a 13, domingo cerrado (como la Clínica Veterinaria Patitas).
    const horarios = [
      ...(['lunes', 'martes', 'miercoles', 'jueves', 'viernes'] as Dia[]).map((d) => franja(d, '09:00:00', '19:00:00')),
      franja('sabado', '09:00:00', '13:00:00'),
    ];
    const grupos = agruparHorarioSemanal(horarios, 'miercoles');
    assert.deepEqual(
      grupos.map((g) => [g.desde, g.hasta, g.franjas.length, g.incluyeHoy]),
      [
        ['lunes', 'viernes', 1, true],
        ['sabado', 'sabado', 1, false],
        ['domingo', 'domingo', 0, false], // sin franjas: no atiende
      ],
    );
  });

  it('una veterinaria que atiende todos los días con el mismo horario es un solo grupo', () => {
    const todos = (['lunes', 'martes', 'miercoles', 'jueves', 'viernes', 'sabado', 'domingo'] as Dia[]).map((d) =>
      franja(d, '00:00:00', '23:59:00'),
    );
    const grupos = agruparHorarioSemanal(todos, 'domingo');
    assert.equal(grupos.length, 1);
    assert.deepEqual([grupos[0].desde, grupos[0].hasta, grupos[0].incluyeHoy], ['lunes', 'domingo', true]);
  });

  it('un día con atención partida (mañana y tarde) lleva las dos franjas en orden', () => {
    const grupos = agruparHorarioSemanal(
      [franja('lunes', '15:00:00', '19:00:00'), franja('lunes', '09:00:00', '13:00:00')],
      'martes',
    );
    assert.deepEqual(grupos[0].franjas, [
      { apertura: '09:00:00', cierre: '13:00:00' },
      { apertura: '15:00:00', cierre: '19:00:00' },
    ]);
  });

  it('agrupa los servicios por categoría respetando el orden del catálogo', () => {
    const grupos = agruparServicios([
      { nombre: 'Vacunación', categoria: 'Medicina preventiva' },
      { nombre: 'Desparasitación', categoria: 'Medicina preventiva' },
      { nombre: 'Cirugía', categoria: 'Especialidad' },
    ]);
    assert.deepEqual(grupos, [
      { categoria: 'Medicina preventiva', servicios: ['Vacunación', 'Desparasitación'] },
      { categoria: 'Especialidad', servicios: ['Cirugía'] },
    ]);
  });
});
