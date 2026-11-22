import assert from 'node:assert/strict';
import { describe, it } from 'node:test';
import { CuidadoPorAvisar, decidirAvisos, fechaLarga } from '../../src/modules/recordatorio/recordatorio.avisos';
import { Dia, diaDeAviso, diaDeLaSemana, diasEntre, sumarDias } from '../../src/modules/recordatorio/recordatorio.dias';

// Octubre de 2026: el 26 es lunes, el 28 miércoles y el 31 sábado.
const LUNES = '2026-10-26';
const MARTES = '2026-10-27';
const MIERCOLES = '2026-10-28';
const DOMINGO = '2026-11-01';

function cuidado(parcial: Partial<CuidadoPorAvisar>): CuidadoPorAvisar {
  return {
    usuarioId: 1,
    cuidadoId: 10,
    cuidado: 'Vacuna contra rabia',
    mascota: 'Luna',
    fechaRecurrente: MIERCOLES,
    avisoAnticipado: true,
    ...parcial,
  };
}
const dias = (...elegidos: Dia[]) => new Map<number, Dia[]>([[1, elegidos]]);

describe('fechas', () => {
  it('reconoce el día de la semana', () => {
    assert.equal(diaDeLaSemana(LUNES), 'lunes');
    assert.equal(diaDeLaSemana(MIERCOLES), 'miercoles');
    assert.equal(diaDeLaSemana(DOMINGO), 'domingo');
  });

  it('suma y resta días cruzando meses y años', () => {
    assert.equal(sumarDias('2026-10-31', 1), '2026-11-01');
    assert.equal(sumarDias('2026-03-01', -1), '2026-02-28');
    assert.equal(sumarDias('2024-03-01', -1), '2024-02-29'); // año bisiesto
    assert.equal(sumarDias('2026-12-31', 1), '2027-01-01');
  });

  it('cuenta los días entre dos fechas', () => {
    assert.equal(diasEntre(LUNES, MIERCOLES), 2);
    assert.equal(diasEntre(MIERCOLES, LUNES), -2);
    assert.equal(diasEntre(LUNES, LUNES), 0);
  });
});

describe('día del aviso anticipado (RF21)', () => {
  it('con todos los días disponibles es el día anterior al vencimiento', () => {
    assert.equal(diaDeAviso(MIERCOLES, new Set()), MARTES);
    assert.equal(diaDeAviso(MIERCOLES, new Set<Dia>(['lunes', 'martes', 'miercoles', 'jueves', 'viernes', 'sabado', 'domingo'])), MARTES);
  });

  it('ejemplo del prototipo: lunes y miércoles, vence el miércoles -> se avisa el lunes', () => {
    assert.equal(diaDeAviso(MIERCOLES, new Set<Dia>(['lunes', 'miercoles'])), LUNES);
  });

  it('ejemplo del prototipo: sábados y domingos, vence el miércoles -> se avisa el domingo anterior', () => {
    assert.equal(diaDeAviso(MIERCOLES, new Set<Dia>(['sabado', 'domingo'])), '2026-10-25');
  });

  it('si el día anterior está disponible, se usa ese', () => {
    assert.equal(diaDeAviso(MIERCOLES, new Set<Dia>(['martes'])), MARTES);
  });

  it('con un solo día disponible busca hacia atrás hasta encontrarlo', () => {
    // Vence el lunes 26 y solo se puede el lunes: el aviso es el lunes de la semana anterior.
    assert.equal(diaDeAviso(LUNES, new Set<Dia>(['lunes'])), '2026-10-19');
  });
});

describe('qué avisos corresponden en un día', () => {
  it('RF22: un cuidado que vence hoy se avisa hoy', () => {
    const avisos = decidirAvisos(MIERCOLES, [cuidado({ fechaRecurrente: MIERCOLES })], new Map());
    assert.equal(avisos.length, 1);
    assert.equal(avisos[0].tipo, 'pendiente');
    assert.equal(avisos[0].titulo, 'Cuidado para hoy');
    assert.equal(avisos[0].cuerpo, 'Luna: Vacuna contra rabia');
  });

  it('RF22: un cuidado vencido se avisa todos los días hasta completarse, con los días que lleva', () => {
    const vencido = cuidado({ fechaRecurrente: LUNES });
    const [deMartes] = decidirAvisos(MARTES, [vencido], new Map());
    assert.equal(deMartes.titulo, 'Cuidado vencido');
    assert.equal(deMartes.cuerpo, 'Luna: Vacuna contra rabia, hace 1 día');
    assert.equal(decidirAvisos(MIERCOLES, [vencido], new Map())[0].cuerpo, 'Luna: Vacuna contra rabia, hace 2 días');
  });

  it('RF22: se avisa aunque el cuidado no tenga aviso anticipado', () => {
    const avisos = decidirAvisos(MARTES, [cuidado({ fechaRecurrente: LUNES, avisoAnticipado: false })], new Map());
    assert.equal(avisos.length, 1);
  });

  it('RF22: los días de disponibilidad no aplican a los avisos diarios', () => {
    // Solo el lunes está disponible, pero el martes igual se avisa el cuidado vencido.
    const avisos = decidirAvisos(MARTES, [cuidado({ fechaRecurrente: LUNES })], dias('lunes'));
    assert.equal(avisos.length, 1);
    assert.equal(avisos[0].tipo, 'pendiente');
  });

  it('RF21: el día antes del vencimiento llega el aviso anticipado', () => {
    const avisos = decidirAvisos(MARTES, [cuidado({ fechaRecurrente: MIERCOLES })], new Map());
    assert.equal(avisos.length, 1);
    assert.equal(avisos[0].tipo, 'anticipado');
    assert.equal(avisos[0].titulo, 'Cuidado próximo a vencer');
    assert.equal(avisos[0].cuerpo, 'Luna: Vacuna contra rabia vence mañana');
  });

  it('RF21: no hay aviso anticipado si el cuidado no lo tiene configurado', () => {
    const avisos = decidirAvisos(MARTES, [cuidado({ fechaRecurrente: MIERCOLES, avisoAnticipado: false })], new Map());
    assert.equal(avisos.length, 0);
  });

  it('RF21: con lunes y miércoles disponibles, un cuidado que vence el miércoles se avisa el lunes y no el martes', () => {
    const elegidos = dias('lunes', 'miercoles');
    const c = cuidado({ fechaRecurrente: MIERCOLES });
    assert.equal(decidirAvisos(LUNES, [c], elegidos).length, 1);
    assert.equal(decidirAvisos(MARTES, [c], elegidos).length, 0);
  });

  it('RF21: el aviso adelantado dice el día de la semana: "este miércoles" si es de esta semana', () => {
    const [aviso] = decidirAvisos(LUNES, [cuidado({ fechaRecurrente: MIERCOLES })], dias('lunes', 'miercoles'));
    assert.equal(aviso.cuerpo, 'Luna: Vacuna contra rabia vence este miércoles');
  });

  it('RF21: si el vencimiento cae en la semana siguiente dice "el próximo" y el día', () => {
    // Sábado 31 de octubre: el lunes 2 de noviembre ya es de la semana siguiente.
    const [deSabado] = decidirAvisos('2026-10-31', [cuidado({ fechaRecurrente: '2026-11-02' })], dias('sabado'));
    assert.equal(deSabado.cuerpo, 'Luna: Vacuna contra rabia vence el próximo lunes');
    // Justo 7 días después: lunes 26 de octubre avisando el lunes 2 de noviembre.
    const [aSieteDias] = decidirAvisos(LUNES, [cuidado({ fechaRecurrente: '2026-11-02' })], dias('lunes'));
    assert.equal(aSieteDias.cuerpo, 'Luna: Vacuna contra rabia vence el próximo lunes');
  });

  it('RF21: no se avisa dos veces en días distintos de la misma ventana', () => {
    const c = cuidado({ fechaRecurrente: MIERCOLES });
    const elegidos = dias('lunes', 'miercoles');
    const diasConAviso = [LUNES, MARTES].filter((hoy) => decidirAvisos(hoy, [c], elegidos).length > 0);
    assert.deepEqual(diasConAviso, [LUNES]);
  });

  it('un cuidado que vence en más de una semana todavía no se avisa', () => {
    const lejano = cuidado({ fechaRecurrente: '2026-12-01' });
    assert.equal(decidirAvisos(MARTES, [lejano], new Map()).length, 0);
  });

  it('cada usuario usa sus propios días', () => {
    const delUno = cuidado({ usuarioId: 1, cuidadoId: 1, fechaRecurrente: MIERCOLES });
    const delDos = cuidado({ usuarioId: 2, cuidadoId: 2, fechaRecurrente: MIERCOLES });
    const porUsuario = new Map<number, Dia[]>([[1, ['lunes']], [2, ['martes']]]);
    const delMartes = decidirAvisos(MARTES, [delUno, delDos], porUsuario);
    assert.deepEqual(delMartes.map((a) => a.usuarioId), [2]);
    const delLunes = decidirAvisos(LUNES, [delUno, delDos], porUsuario);
    assert.deepEqual(delLunes.map((a) => a.usuarioId), [1]);
  });

  it('el aviso lleva el nombre de la mascota y del cuidado', () => {
    const [aviso] = decidirAvisos(MIERCOLES, [cuidado({ mascota: 'Michi', cuidado: 'Baño' })], new Map());
    assert.match(aviso.cuerpo, /Michi/);
    assert.match(aviso.cuerpo, /Baño/);
  });
});

describe('texto de la fecha', () => {
  it('escribe día de la semana, día y mes', () => {
    assert.equal(fechaLarga('2026-10-28'), 'miércoles 28 de octubre');
    assert.equal(fechaLarga('2027-01-05'), 'martes 5 de enero');
  });
});
