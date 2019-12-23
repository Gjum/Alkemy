import React, { Fragment } from 'react';

export class ContainerInfo extends React.Component {
	constructor(props) {
		super(props)
		this.state = {}
	}

	render() {
		const { name, maxVolume, temperature, mass, liquidVolume } = this.props.container
		const liquid = {
			'Water': 123,
			'aa/a\\a': 42,
			'Slag': 1,
		}
		return <div className='container-info'>
			<div className='container-info-col container-props'>
				<h3 className='container-info-heading container-info-heading-primary'>
					{name}</h3>
				<dl>
					<dt>Max. Volume</dt><dd>{maxVolume}&nbsp;ml</dd>
					<dt>Temperature</dt><dd>{temperature}&nbsp;K</dd>
					<dt>Total Mass</dt><dd>{mass}&nbsp;g</dd>
					<dt>Liquid Volume</dt><dd>
						{liquidVolume}&nbsp;ml <span className='container-info-hint'>/&nbsp;{maxVolume}ml</span>
					</dd>
				</dl>
			</div>
			<div className='container-info-col container-composition-liquid'>
				<h4 className='container-info-heading'>Liquid composition:</h4>
				<dl>
					{Object.entries(liquid).map(([molecule, mass], i) => <Fragment>
						<dt>{molecule}</dt><dd>{mass}&nbsp;g</dd>
					</Fragment>)}
				</dl>
			</div>
		</div>
	}
}

export default ContainerInfo;
