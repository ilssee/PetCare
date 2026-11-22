import { test } from 'node:test';
import assert from 'node:assert/strict';
import { agruparAvisos, Coincidencia } from '../../src/modules/recomendacion/recomendacion.avisos';

const c = (mascotaId: number, mascota: string, reglaId: number, usuarioId = 1): Coincidencia => ({
  usuarioId,
  mascotaId,
  mascota,
  reglaId,
});

test('RF26: sin coincidencias no hay avisos', () => {
  assert.deepEqual(agruparAvisos([]), []);
});

test('RF26: una recomendación nueva se avisa en singular', () => {
  const [unico] = agruparAvisos([c(10, 'Luna', 29)]);
  assert.equal(unico.aviso.titulo, 'Nuevas recomendaciones para Luna');
  assert.equal(unico.aviso.cuerpo, 'Tenemos 1 nuevo cuidado preventivo para recomendarte.');
  assert.equal(unico.aviso.tipo, 'recomendacion');
  assert.equal(unico.aviso.cuidadoId, 0);
  assert.deepEqual(unico.reglasIds, [29]);
});

test('RF26: el aviso lleva la mascota y sus recomendaciones nuevas, para abrirlas al tocarlo', () => {
  const [aviso] = agruparAvisos([c(10, 'Luna', 29), c(10, 'Luna', 30)]);
  assert.equal(aviso.aviso.mascotaId, 10);
  assert.deepEqual(aviso.aviso.reglasIds, [29, 30]);
});

test('RF26: varias recomendaciones de la misma mascota se juntan en un solo aviso', () => {
  const avisos = agruparAvisos([c(10, 'Luna', 29), c(10, 'Luna', 30), c(10, 'Luna', 31)]);
  assert.equal(avisos.length, 1);
  assert.equal(avisos[0].aviso.cuerpo, 'Tenemos 3 nuevos cuidados preventivos para recomendarte.');
  assert.deepEqual(avisos[0].reglasIds, [29, 30, 31]);
});

test('RF26: cada mascota recibe su propio aviso, dirigido a su dueño', () => {
  const avisos = agruparAvisos([c(10, 'Luna', 29, 1), c(11, 'Max', 29, 2)]);
  assert.equal(avisos.length, 2);
  assert.deepEqual(avisos.map((a) => a.aviso.usuarioId), [1, 2]);
  assert.deepEqual(avisos.map((a) => a.mascotaId), [10, 11]);
});
