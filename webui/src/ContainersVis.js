import React from 'react';
import { Flask } from './FlaskVis';
import { Canister } from './CanisterVis';
import ContainerInfo from './ContainerInfo';

const containerVisClasses = { Flask, Canister }

export class ContainersVis extends React.Component {
	constructor(props) {
		super(props)
		this.state = {}
	}

	render() {
		const { containers, wiring } = this.props
		return <div className='ContainersVis'>
			{containers.map((container, i) => {
				const ContainerVisClass = containerVisClasses[container.type]
				return <div className='container-vis' key={i}>
					{ContainerVisClass
						? <ContainerVisClass container={container} wiring={wiring} />
						: <svg className="container-vis-svg unknown-vis" viewBox='0 0 200 200'>
							<text
								x={100} y={150} textAnchor="middle"
								fill='darkred' fontSize='150px'
							>?</text></svg>
					}
					<ContainerInfo container={container} />
				</div>
			})}
		</div>
	}
}

export default ContainersVis;
